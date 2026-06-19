# Architecture

ImmunoCircuit is organized into six layers.

## 1. Model layer

The model layer contains simple Java objects representing immune-network concepts.

Important classes:

- `ImmuneNode`
- `NodeType`
- `SignalState`
- `InfluenceEdge`
- `RuleTrace`

(`RuleStatement`, the parsed form of a `RULE` declaration, lives in the language layer's AST, not here — a rule is part of the program, not part of the biological model.)

The model is intentionally small and readable. The project is not trying to reproduce full immunology. It is trying to show that a biological domain can be represented with clear software objects.

## 2. Input/output layer

The `io` package loads CSV files for the catalog and network.

- `CatalogLoader` loads the interleukin catalog.
- `NetworkLoader` loads directed relationships between immune nodes.

## 3. Network/algorithm layer

The `ImmuneNetwork` class stores nodes in a `HashMap` for fast symbol lookup and stores edges in an adjacency list for graph traversal.

Supported algorithms include:

- score ranking,
- adjacency traversal,
- feedback-loop detection using DFS.

## 4. Language layer

The `lang` package contains the ICirc Script implementation.

Pipeline:

```text
source text -> Lexer -> tokens -> Parser -> AST statements -> SemanticAnalyzer -> Interpreter
```

The parser is a handwritten recursive-descent parser. This keeps the compiler/interpreter logic visible for a portfolio reviewer.

Four exception types map onto four distinct pipeline phases, on purpose:

| Phase | Exception | Example |
|---|---|---|
| Lexing | `LexerException` | an unrecognized character |
| Parsing | `ParserException` | `SET IL6 HIGH;` (missing `=`) — grammatically broken |
| Semantic analysis | `SemanticException` | `SET IL6X = HIGH;` — grammatically fine, but `IL6X` resolves to nothing |
| Execution | `InterpreterException` | a negative step count, an unreadable LOAD path |

A script can be syntactically perfect and still be semantically wrong — `SemanticException` exists specifically to make that distinction visible instead of folding it into a generic runtime error.

## 5. Semantic analysis layer

`SemanticAnalyzer` is **not** a single whole-program pre-pass that fully resolves the AST before anything runs. It is staged: each check runs after parsing and immediately before the specific statement it guards takes effect. `SET` is checked at the moment it executes; rules are checked as a batch right before `RUN` executes the cascade, not when they are parsed or loaded. This is a smaller claim than "the whole program is resolved up front," and it is the honest one for an interpreter this size — but it is still a real, distinct phase: a script can pass parsing and still be rejected here before it has any effect on network state. It performs two checks that the grammar alone cannot express:

- **`SET` targets must already be known, once there is something to know.** If a catalog or network CSV has been loaded, `SET` assigns an initial condition to an existing node and `SemanticAnalyzer.validateSetTarget` rejects unknown symbols — catching typos like `SET IL6X = HIGH;` immediately instead of silently fabricating a phantom node. If nothing has been loaded, there is no ground truth to typo against, so `SET` is allowed to declare a node directly; this keeps "exercise one rule with no CSV" a legitimate, uncluttered workflow.
- **Rule conditions must resolve, and rule names must be unique.** `SemanticAnalyzer.validateRules` builds a symbol table from every node the network already knows plus every rule's *action target* (a rule target is a legitimate declaration site — it is how synthetic signal/cell-program nodes like `NFkB` or `Th17` enter the program if they were not already in the catalog or network CSV). Every predicate symbol in every rule condition must appear in that table, and run before the cascade is interpreted, before any rule fires.

This is the project's symbol-table-equivalent: instead of resolving variable references against lexical scope (as a general-purpose language would), it resolves node references against the catalog/network/rule-target namespace.

## 6. Explanation layer

The interpreter records `RuleTrace` objects whenever a rule fires. This lets the user ask for explanations after a simulation run.

Example:

```text
EXPLAIN Th17;
```

The output shows the node state, score, and rule trace.

## Design principle

The project uses a larger catalog but a smaller scenario. The catalog can contain all official interleukin genes, while the default scenario only actively models a focused Th17/inflammatory circuit. This prevents scope creep while keeping the program extensible.
