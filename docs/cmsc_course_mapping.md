# CMSC Course Mapping

This document explains how ImmunoCircuit demonstrates concepts from CMSC 115, CMSC 215, CMSC 315, and CMSC 330.

## CMSC 115: Java fundamentals

ImmunoCircuit demonstrates:

- variables and data types,
- conditionals,
- loops,
- methods,
- file input,
- arrays/lists,
- debugging-friendly decomposition,
- readable documentation.

Examples:

- `CatalogLoader` reads CSV rows.
- `Main` handles command-line input.
- `Interpreter` coordinates program execution.

## CMSC 215: Object-oriented programming

ImmunoCircuit demonstrates:

- classes and objects,
- encapsulation,
- enums,
- exception handling,
- interface-based AST design,
- reusable model classes.

Examples:

- `ImmuneNode` models a biological node.
- `Expression` is an interface for rule conditions.
- `Statement` is an interface for script commands.
- `LexerException`, `ParserException`, `SemanticException`, and `InterpreterException` form a small exception hierarchy, each scoped to exactly one pipeline phase rather than one catch-all "something went wrong."

## CMSC 315: Data structures and algorithms

ImmunoCircuit demonstrates:

- hash maps for symbol lookup,
- adjacency-list graphs,
- depth-first search,
- sorting and ranking,
- complexity analysis.

Examples:

- `ImmuneNetwork` stores nodes in a `HashMap`.
- `findFeedbackLoops()` uses DFS.
- `rankByScore()` sorts nodes by score.

## CMSC 330: Programming languages

ImmunoCircuit demonstrates:

- syntax and semantics, kept as distinct phases with distinct exception types,
- lexical analysis,
- recursive-descent parsing with single-token lookahead (LL(1)),
- AST construction,
- a separate semantic-analysis phase (symbol resolution),
- interpretation,
- imperative vs declarative language comparison.

Examples, with file pointers:

- `lang/Lexer.java` — converts ICirc source into tokens; `scanToken()` is the character dispatch, `KEYWORDS` is the reserved-word table.
- `lang/Parser.java` — recursive-descent parser; `condition()` -> `orExpression()` -> `andExpression()` -> `predicate()` is textbook operator-precedence parsing implemented by hand, not generated. Every nonterminal in `docs/grammar.md` has a matching private method of the same name.
- `lang/ast/Expression.java`, `PredicateExpression.java`, `BinaryExpression.java` — the AST for rule conditions; `evaluate()` is the tree-walking interpreter step, `referencedSymbols()` is what the semantic analyzer walks to resolve names.
- `lang/SemanticAnalyzer.java` — the symbol-table-equivalent phase. `validateSetTarget` rejects unresolved `SET` targets; `validateRules` rejects duplicate rule names and unresolved predicate symbols. This is what makes "semantic error" a real, distinct category in this project instead of a synonym for "runtime error."
- `lang/LexerException.java`, `ParserException.java`, `SemanticException.java`, `InterpreterException.java` — four exception types, one per pipeline phase (lex, parse, resolve, execute). See `docs/architecture.md` for the phase-to-exception table.
- `Interpreter.java` — `Statement.execute(Interpreter)` is the GoF Interpreter pattern: each AST node knows how to evaluate itself against an `Interpreter`, rather than the interpreter switching on node type.
- Rule files (`rules/th17_core.icirc`) express immune logic declaratively (no control flow, no order-dependence beyond per-step evaluation), while `Interpreter.runSteps` executes that logic with an ordinary imperative Java loop — the project deliberately juxtaposes both styles.

## Portfolio takeaway

The project is designed to show growth across the curriculum:

```text
Java basics -> object-oriented design -> data structures -> programming-language implementation
```

The immunology theme is the domain. The computer science architecture is the portfolio value.
