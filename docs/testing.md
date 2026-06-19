# Testing Plan

ImmunoCircuit includes a small JUnit test suite and can also be manually tested through example scripts.

## Test categories

### 1. Lexer tests (`LexerTest`)

Goal: verify that source text becomes the expected token sequence, and that malformed input fails at the lexical stage with a `LexerException` rather than surfacing as a confusing parser or interpreter error.

Covered:

- `SET IL6 = HIGH;` produces the expected `SET IDENTIFIER EQUAL IDENTIFIER SEMICOLON` token sequence,
- keywords are matched case-insensitively (`set` == `SET`) while identifiers preserve case,
- an unrecognized character (`@`) raises `LexerException`,
- an unterminated string literal raises `LexerException`.

### 2. Parser tests (`ParserTest`)

Goal: verify that tokens become valid AST statements with correct structure, and that malformed-but-tokenizable input fails with a `ParserException` naming what was expected.

Covered:

- parsing a `RULE` statement into a `RuleStatement`,
- `AND` binds tighter than `OR` (`A AND B OR C` parses as `(A AND B) OR C`), checked against the AST's own `describe()` output rather than re-deriving the tree by hand,
- a `SET` statement missing `=` raises `ParserException`,
- a `RULE` missing `THEN` raises `ParserException`.

### 3. Network tests (`NetworkTest`)

Goal: verify graph behavior — construction, traversal, and the two algorithms layered on top of it.

Covered:

- a 3-node cycle is detected exactly once, not once per rotation (exercises the cycle-key canonicalization in `findFeedbackLoops()`, not just "a cycle is found"),
- an acyclic graph reports zero feedback loops,
- ranking sorts by score descending with a symbol tie-break for equal scores,
- a `LIMIT` truncates the ranked list.

### 4. Semantic analysis tests (`InterpreterTest`)

Goal: verify that the semantic-analysis phase (`SemanticAnalyzer`) catches what the grammar cannot, and that it does so without breaking legitimate scripts.

Covered:

- `SET` on an unresolved symbol is rejected once a catalog/network is loaded (typo protection),
- `SET` is still allowed to declare a synthetic node when nothing has been loaded (the no-CSV, single-rule sandbox workflow),
- a rule condition referencing an unresolved predicate symbol is rejected,
- duplicate rule names are rejected,
- a rule's action target is correctly treated as a declaration site — a later rule may read it as a predicate without triggering a false "unknown symbol" error.

### 5. Interpreter tests (`InterpreterTest`)

Goal: verify end-to-end rule execution.

Example:

```text
RULE demo:
IF IL6 HIGH THEN STAT3 ACTIVE +2;
```

After setting `IL6 = HIGH` and running one step, `STAT3` should be active and have score 2.

### 6. Integration / manual tests

Goal: run the example script and confirm that major output sections appear.

```bash
./build.sh
./run.sh examples/th17_demo.icirc
```

## Future testing improvements

- Golden-file tests that diff the full output of `examples/th17_demo.icirc` against a checked-in expected file, to catch accidental output-format regressions.
- Mutation-style tests for rule behavior (e.g. flip a `+` to a `-` in a rule and confirm some test fails).
- A test that exercises `LOAD RULES` end to end against `rules/th17_core.icirc` directly, rather than only inline rule strings.
