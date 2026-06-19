# ImmunoCircuit

**ImmunoCircuit** is a Java portfolio project that demonstrates object-oriented programming, data structures, algorithm analysis, and programming-language design through a synthetic immune-signaling network.

The project combines:

- an HGNC-based interleukin catalog starter dataset,
- a focused Th17 / inflammatory-regulatory circuit,
- graph-based immune network analysis,
- a small domain-specific language called **ICirc Script**,
- a lexer, recursive-descent parser, AST, semantic analyzer, and interpreter,
- explainable rule traces showing why simulated nodes became active, suppressed, or scored.

> **Educational disclaimer:** ImmunoCircuit uses simplified synthetic rules for computer science demonstration only. It is not a clinical, diagnostic, or research-grade biological model.

---

## Why this project exists

Most student biomedical portfolio projects become a search app, a CRUD database, or a symptom checker. ImmunoCircuit is different: it treats immunology as a domain for showing core computer science ideas.

The main design question is:

> Can a small custom language describe immune-signaling rules, and can a Java interpreter evaluate those rules over a graph-based cytokine network while producing readable explanations?

---

## Quick start

### Option A: compile with the included shell script

```bash
chmod +x build.sh run.sh
./build.sh
./run.sh examples/th17_demo.icirc
```

### Option B: compile manually

```bash
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
java -cp out immunocircuit.Main examples/th17_demo.icirc
```

### Option C: Maven layout

This repository also includes a `pom.xml` so it can be imported into IntelliJ, Eclipse, or VS Code as a Maven-style Java project.

---

## Example ICirc Script

```text
LOAD CATALOG FROM "data/hgnc_interleukins.csv";
LOAD NETWORK FROM "data/th17_network.csv";
LOAD RULES FROM "rules/th17_core.icirc";

SET IL6 = HIGH;
SET IL23A = HIGH;
SET TGFB1 = MODERATE;
SET IL10 = LOW;
SET TNF = HIGH;

RUN 3 STEPS;

EXPLAIN Th17;
EXPLAIN RORC;
RANK NODES BY SCORE DESC LIMIT 10;
FIND FEEDBACK_LOOPS;
REPORT UNMODELED_GENES;
```

---

## Example output

This is the actual output of `./run.sh examples/th17_demo.icirc` against the current rule set (full output, including the parts truncated below, is in `docs/demo_output.md`):

```text
Loaded catalog: data/hgnc_interleukins.csv (43 rows)
Loaded network: data/th17_network.csv (23 edges)
Loaded rules: rules/th17_core.icirc (12 total rules)
Set IL6 = HIGH
Set IL23A = HIGH
Set TGFB1 = MODERATE
Set IL10 = LOW
Set TNF = HIGH
Set IL36G = HIGH
Set IL37 = LOW
Ran cascade for 3 step(s).

Explanation for Th17
Name: Th17
Type: CELL_PROGRAM
State: ACTIVE
Score: 18
Rules fired:
  step 1: th17_stabilization: (IL23A was HIGH AND RORC was ACTIVE) -> Th17 ACTIVE +3
  step 1: th17_program: RORC was ACTIVE -> Th17 ACTIVE +3
  step 2: th17_stabilization: (IL23A was HIGH AND RORC was ACTIVE) -> Th17 ACTIVE +3
  step 2: th17_program: RORC was ACTIVE -> Th17 ACTIVE +3
  step 3: th17_stabilization: (IL23A was HIGH AND RORC was ACTIVE) -> Th17 ACTIVE +3
  step 3: th17_program: RORC was ACTIVE -> Th17 ACTIVE +3
Rules blocked:
  step 1: treg_counterbalance: FOXP3 was ABSENT, expected ACTIVE
  step 2: treg_counterbalance: FOXP3 was ABSENT, expected ACTIVE
  step 3: treg_counterbalance: FOXP3 was ABSENT, expected ACTIVE
```

Two rules (`th17_stabilization` and `th17_program`) both activate `Th17` and both fire on every one of the 3 steps, which is why the score is 18 (`(3 + 3) * 3`), not 9 — there is no rule-firing dedup or convergence check, by design, so a node can keep accumulating score across steps as long as its condition stays true. `Rules blocked` shows the converse case: `treg_counterbalance` would suppress `Th17`, but its `IF FOXP3 ACTIVE` condition never holds in this scenario, and `EXPLAIN` says exactly why instead of omitting the rule silently. See `docs/demo_output.md` for the full run and `docs/known_tradeoffs.md` for why scores accumulate this way.

---

## Course concept mapping

| Course | Concepts demonstrated |
|---|---|
| CMSC 115 | Java fundamentals, methods, control flow, input/output, arrays/lists, debugging, decomposition |
| CMSC 215 | Object-oriented design, encapsulation, interfaces, inheritance-friendly model classes, exceptions, documentation |
| CMSC 315 | Hash maps, adjacency-list graphs, DFS cycle detection, sorting, score ranking, Big-O analysis |
| CMSC 330 | Syntax, semantics, lexical analysis, recursive-descent parsing, ASTs, semantic analysis/symbol resolution, interpreter design, imperative vs declarative comparison |

---

## Repository layout

```text
immunocircuit/
├── README.md
├── pom.xml
├── build.sh
├── run.sh
├── data/
│   ├── hgnc_interleukins.csv
│   └── th17_network.csv
├── rules/
│   └── th17_core.icirc
├── examples/
│   └── th17_demo.icirc
├── docs/
│   ├── architecture.md
│   ├── grammar.md
│   ├── semantics.md
│   ├── complexity.md
│   ├── testing.md
│   ├── cmsc_course_mapping.md
│   ├── known_tradeoffs.md
│   ├── demo_output.md
│   └── references.md
└── src/
    ├── main/java/immunocircuit/
    └── test/java/immunocircuit/
```

---

## Main features

### 1. Controlled catalog layer

The starter catalog file contains 43 interleukin-related genes from the HGNC Interleukins group as a controlled vocabulary layer. The simulator can validate symbols against this catalog and distinguish between catalog-only genes and genes actively modeled in a scenario. The catalog's alias column is resolved too — `SET IL40 = HIGH;` and `SET C17orf99 = HIGH;` mutate the same node, because the alias index built from the catalog maps `IL40` to its canonical symbol.

### 2. Scenario layer

The default scenario focuses on a manageable Th17 / inflammatory-regulatory circuit rather than pretending to model all immune biology.

### 3. Graph layer

The immune network is stored as an adjacency-list graph. It supports:

- fast node lookup,
- directed pathway relationships,
- score ranking,
- feedback-loop detection.

### 4. Language layer

ICirc Script is intentionally small but complete enough to demonstrate a programming-language pipeline:

```text
source code -> lexer -> tokens -> parser -> AST -> semantic analyzer -> interpreter -> network state
```

Semantic checks run after parsing and before the affected statement takes effect — `SET` is checked when it executes, rules are checked as a batch right before `RUN` executes the cascade. It is not a single whole-AST resolution pass; it is staged, the same way the interpreter itself is. The checks themselves are symbol resolution and rule-name validation, not general type checking: `SET` must target a symbol that's already known once a catalog/network is loaded, and a rule condition must read only symbols that resolve to a known node or another rule's action target. A script can be grammatically perfect and still get rejected here — that distinction is the point. See `docs/semantics.md`, `docs/architecture.md`, and `docs/known_tradeoffs.md`.

### 5. Explainability layer

Every rule evaluation is logged, whether it fired or not. `EXPLAIN` shows two sections: **rules fired** (which rules changed this node's state or score, and when), and **rules blocked** (which rules target this node but didn't fire, and which specific predicate was false — e.g. `IL10 was LOW, expected HIGH` — not just "condition failed"). `EXPLAIN` is itself a semantic use site: once a catalog or network is loaded, asking to explain an unresolved symbol raises a `SemanticException` rather than printing an empty report.

---

## Sample rule file

An excerpt of `rules/th17_core.icirc` (see that file for the full 12-rule set):

```text
RULE th17_differentiation:
IF STAT3 ACTIVE AND TGFB1 MODERATE THEN RORC ACTIVE +2;

RULE th17_stabilization:
IF IL23A HIGH AND RORC ACTIVE THEN Th17 ACTIVE +3;

RULE th17_program:
IF RORC ACTIVE THEN Th17 ACTIVE +3;

RULE regulatory_brake:
IF IL10 HIGH THEN TNF SUPPRESSED -2;
```

---

## Safety and scope

This project intentionally avoids medical inference. The biological relationships are simplified and synthetic. The goal is to demonstrate software design, parsing, graph modeling, and algorithmic reasoning.

---

## Suggested next improvements

- Add parentheses and NOT to the rule grammar.
- Add a Swing or JavaFX visualization for the pathway graph.
- Add more scenario packs, such as IL-1 family inflammation or type 2 immunity.
- Add a Prolog comparison file to contrast declarative logic programming with the Java interpreter.
- Replace the starter catalog with a reproducible HGNC download pipeline.
