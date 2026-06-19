# Semantics

This document explains what ICirc Script statements mean once parsed.

## Program state

The interpreter maintains:

- an `ImmuneNetwork`,
- a list of rules,
- node states,
- node scores,
- rule traces.

## LOAD CATALOG

```text
LOAD CATALOG FROM "data/hgnc_interleukins.csv";
```

Loads gene symbols into the network as valid nodes. Catalog nodes may remain unmodeled if they are not used in the current scenario.

## LOAD NETWORK

```text
LOAD NETWORK FROM "data/th17_network.csv";
```

Loads directed edges between nodes. If an edge references a node not already in the catalog, the loader creates a supporting node.

## LOAD RULES

```text
LOAD RULES FROM "rules/th17_core.icirc";
```

Reads a rule file, lexes it, parses it into rule statements, and stores the resulting rules in the interpreter.

## SET

```text
SET IL6 = HIGH;
```

Updates the current state of the node `IL6` to `HIGH`.

This is a semantic check, not just an assignment. Once a catalog or network CSV has been loaded, `SemanticAnalyzer.validateSetTarget` requires the target to already exist in the network — `SET IL6X = HIGH;` raises a `SemanticException` rather than silently creating a node called `IL6X`, because there is real data to typo against.

If no catalog or network has been loaded at all, `SET` is allowed to introduce a node directly (`ImmuneNetwork.hasLoadedData()` reports `false`, so there is no ground truth to check against). This is what lets a single rule be exercised in isolation — `SET CUSTOM_NODE = HIGH; RULE r: IF CUSTOM_NODE HIGH THEN ...` — without LOADing any CSV, the same way a unit test exercises one function without booting the whole program.

## RUN

```text
RUN 3 STEPS;
```

Before the cascade runs, `SemanticAnalyzer.validateRules` resolves every loaded rule. It requires:

- every rule name to be unique,
- every symbol read in a rule's `IF` condition to already be known — either because it is a catalog/network node, or because some rule's `THEN` target declares it.

Only after that check passes does the interpreter run the cascade three times. On each step, rules are evaluated in file order. If a rule condition is true, the rule action is applied.

**ICirc uses immediate-update semantics.** A rule's action takes effect the moment it fires — not at the end of the step. Because rules are evaluated in file order within a step, a later rule in the same step can observe an earlier rule's effect from that same step. This is why, for example, `rules/th17_core.icirc` can chain `il23_receptor_present` (`IF IL23A HIGH THEN IL23R PRESENT +1;`) into `il23_to_stat3` (`IF IL23A HIGH AND IL23R PRESENT THEN STAT3 ACTIVE +2;`) and have both fire on step 1: `IL23R` becomes `PRESENT` when the first rule fires, and the second rule's predicate sees that updated state immediately, within the same step, rather than having to wait for step 2. This is a deliberate interpreter design choice — not a bug — but it does mean rule *order* in the file is semantically meaningful, which is worth stating plainly rather than leaving implicit.

## Rule condition

```text
IF IL6 HIGH AND TGFB1 MODERATE
```

A predicate is true when the referenced node currently has the requested state.

`AND` requires both sides to be true. `OR` requires at least one side to be true.

A rule's `THEN` target is treated differently from the symbols its `IF` reads: the target is a *declaration site* (it is how a rule introduces a synthetic node such as `NFkB` that no CSV defines), so it is never rejected as unknown. A symbol read by `IF`, by contrast, is a *use site* and must already resolve to something — either a loaded node or another rule's target.

## Rule action

```text
THEN RORC ACTIVE +2;
```

The target node's state becomes `ACTIVE`, and its score increases by 2. The interpreter records a trace explaining which rule fired.

## EXPLAIN

```text
EXPLAIN Th17;
```

Like `SET`, this is a semantic use site: once a catalog or network is loaded, `SemanticAnalyzer.validateExplainTarget` rejects an unresolved symbol with a `SemanticException` rather than printing an empty "no node found" message. The catalog's alias column is part of what counts as resolvable — `EXPLAIN IL40;` finds the same node as `EXPLAIN C17orf99;` if the catalog lists `IL40` as an alias. The same alias resolution applies to `SET`, rule predicates, rule actions, and network edges — see `docs/known_tradeoffs.md` for the one real gap: aliases containing spaces or punctuation (`IL-40`, `interleukin 40`) aren't valid ICirc identifiers, so only identifier-safe aliases like `IL40` can actually be written in a script.

Prints:

- current state,
- current score,
- **rules fired** — every rule application that changed this node, with the step it happened on,
- **rules blocked** — every rule whose action targets this node but whose condition evaluated false, with the specific predicate that failed (e.g. `IL10 was LOW, expected HIGH`) rather than just restating the whole condition.

## RANK

```text
RANK NODES BY SCORE DESC LIMIT 10;
```

Sorts nodes by score and prints the top results.

## FIND FEEDBACK_LOOPS

Runs cycle detection over the adjacency-list graph and reports directed feedback loops.

## REPORT UNMODELED_GENES

Compares catalog nodes against scenario-modeled nodes. This makes scope control explicit:

```text
Loaded catalog genes: 43
Modeled in current scenario: 10
Catalog-only genes: 33
```
