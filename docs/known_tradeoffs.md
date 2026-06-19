# Known Tradeoffs

This project makes several deliberate simplifications. Each one is a real design decision with a stated reason, not an oversight — listed here so a reviewer doesn't have to reverse-engineer the intent from the code, and so future work has an honest starting point instead of re-discovering the same tradeoff.

## `SignalState` is one unified enum, not two

`SignalState` mixes level states (`ABSENT`, `LOW`, `MODERATE`, `HIGH`) with condition states (`PRESENT`, `ACTIVE`, `SUPPRESSED`) in a single enum. This means `IF IL6 ACTIVE` is grammatically and semantically legal even though "a cytokine is active" doesn't really mean anything in the model — `IL6` should only ever be compared against level states.

A stricter dialect would split this into two enums (or a `Kind` discriminator) and have `SemanticAnalyzer` reject a `NodeType`/`SignalState` combination that doesn't make sense. This project does not do that, because the existing rule set genuinely needs to cross the line: `regulatory_brake` (`IF IL10 HIGH THEN TNF SUPPRESSED -2;`) intentionally drives a cytokine (`TNF`) into a condition state (`SUPPRESSED`) to model regulatory suppression. Splitting the enum would mean redesigning that rule, not just the type, so it's left as a single enum with the tradeoff documented in `SignalState`'s Javadoc.

**What this is not:** a type-checking layer. `SemanticAnalyzer` does symbol resolution and rule-name validation — it answers "does this name refer to something?" and "is this rule name unique?", not "is this state appropriate for this kind of node?". Don't describe it as type checking; that's a different, larger feature this project doesn't have.

## Rule action targets are declaration sites; predicate symbols are use sites

A rule's `THEN` target (e.g. `Th17` in `... THEN Th17 ACTIVE +3;`) is allowed to be a symbol that no CSV ever defines — it's how synthetic signal/cell-program nodes enter the program. A rule's `IF` predicate symbol, by contrast, must already resolve to something: either a catalog/network node, or some other rule's action target.

This asymmetry is intentional and is the closest thing this project has to "declaring vs. using a variable." It is also why `SET` behaves differently depending on whether any data has been loaded yet (see below) — the same declare/use distinction shows up there too.

## Semantic checks are staged, not a whole-program pre-pass

`SemanticAnalyzer` does not resolve the entire AST before any statement executes. The checks happen after parsing and immediately before the specific statement they guard takes effect:

- `SET` is validated the moment it executes (`Interpreter.setState`).
- Rule conditions and rule-name uniqueness are validated as a batch immediately before `RUN` executes the cascade (`Interpreter.runSteps`), not when each `RULE` statement is parsed or loaded.

This is a smaller, more honest claim than "the program is fully resolved before it runs," and it matches how small interpreters are usually built — but it does mean a script that loads data, runs once successfully, then loads a second rule file with a bad reference won't be caught until the *next* `RUN`, not at `LOAD RULES` time. For a script this size that's a reasonable place to stop; a larger version of this project would want a true pre-execution resolution pass over the entire program before any `LOAD` runs.

## `SET` is strict only once real data exists

If a catalog or network CSV has been loaded, `SET` requires its target to already be a known node — `SET IL6X = HIGH;` is rejected as a likely typo. If nothing has been loaded yet, `SET` is allowed to introduce a node directly, because there's no ground truth to check a name against. This lets a script (or a test) exercise a single rule in isolation without loading any CSV — see `executesSmallRuleProgram` and the sandbox-related tests in `InterpreterTest`.

The practical effect: typo protection only kicks in for "real" scenario scripts (the ones that `LOAD CATALOG`/`LOAD NETWORK`), not for minimal rule-testing scripts. That's the intended scope — the goal is catching typos against real biology data, not preventing all ad hoc node creation everywhere.

## Ranking is a full sort, not a bounded heap

`ImmuneNetwork.rankByScore()` sorts every node (`O(V log V)`) and then truncates to `LIMIT`. A bounded `PriorityQueue` of size `k` would be `O(V log k)`, which matters once `V` is large relative to `k`. This project's `V` is small (tens of nodes), so the full sort is not a real bottleneck, and "score ranking," not "top-k ranking," is the accurate description of what's implemented today. See `docs/complexity.md` for the cost comparison if this is changed later.

## Alias resolution is uniform across SET, predicates, rule actions, EXPLAIN, and network edges

`ImmuneNetwork.findNode()` resolves an alias (e.g. `IL40`) to its catalog-declared canonical symbol (`C17orf99`). Every entry point that needs to resolve a symbol to a node — `setState`, `applyRuleAction`, `addEdge`, and `explain` — goes through `findNode()` (directly or via the private `resolveOrCreate()` helper), and `SemanticAnalyzer.validateRules()` checks predicate symbols with `network.findNode(symbol).isPresent()` rather than a pre-collected set of canonical symbols. That last point used to be a real bug: an earlier version built the declared-symbol set from `node.getSymbol()` values, which are always canonical, so `IF IL40 HIGH` was rejected as unknown even though `SET IL40 = HIGH;` and `EXPLAIN IL40;` both resolved it correctly. Fixed by checking the network directly instead of a derived set — see `InterpreterTest.aliasResolvesInsideRulePredicateNotJustSetAndExplain()`.

## Aliases with spaces or punctuation aren't valid script identifiers

The catalog's alias column stores things like `IL-40` and `interleukin 40` alongside identifier-safe aliases like `IL40`. ICirc identifiers (the lexer's `identifier()` rule) only accept letters, digits, and underscores — so `SET IL-40 = HIGH;` does not parse: the lexer reads `IL`, then a `-` token, then `40`, and the parser rejects the resulting token stream. `SET IL40 = HIGH;` works because that particular alias happens to be identifier-safe.

This is a real, current limitation, not a bug: the catalog data legitimately contains aliases the language can't reference by name yet. A future grammar extension could accept quoted symbols (`SET "IL-40" = HIGH;`, `EXPLAIN "interleukin 40";`) to close the gap, but that's a deliberate language-design decision — a new lexer rule and a parser change — not something to bolt on casually. Not planned for now.

## A rule fires every step its condition holds — there is no convergence/dedup guard

If a rule's condition stays true across multiple steps of `RUN n STEPS;`, it fires on every one of those steps and its score delta accumulates each time. There's no "already fired, don't re-apply" tracking per rule per node. This is why, in the bundled demo, `Th17`'s score is 18 rather than some smaller "fires once" number — see `docs/demo_output.md` for the worked example. This is intentional: it keeps the interpreter's state small (no extra bookkeeping per rule per step) and mirrors how a real repeated-stimulus cascade would keep reinforcing an active pathway.
