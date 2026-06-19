# ICirc Script Grammar

ICirc Script is a small domain-specific language for loading immune-network data, setting initial cytokine states, running a simplified cascade, and asking for explanations.

## Informal grammar

```text
program      -> statement* EOF ;

statement    -> loadStmt
              | setStmt
              | runStmt
              | explainStmt
              | rankStmt
              | findStmt
              | reportStmt
              | ruleStmt ;

loadStmt     -> "LOAD" loadKind "FROM" STRING ";" ;
loadKind     -> "CATALOG" | "NETWORK" | "RULES" ;

setStmt      -> "SET" IDENT "=" state ";" ;
state        -> "ABSENT" | "LOW" | "MODERATE" | "HIGH" | "PRESENT" | "ACTIVE" | "SUPPRESSED" ;

runStmt      -> "RUN" NUMBER "STEPS" ";" ;

explainStmt  -> "EXPLAIN" IDENT ";" ;

rankStmt     -> "RANK" "NODES" "BY" "SCORE" order? "LIMIT" NUMBER ";" ;
order        -> "ASC" | "DESC" ;

findStmt     -> "FIND" "FEEDBACK_LOOPS" ";" ;

reportStmt   -> "REPORT" "UNMODELED_GENES" ";" ;

ruleStmt     -> "RULE" IDENT ":" "IF" condition "THEN" action ";" ;
condition    -> orExpr ;
orExpr       -> andExpr ("OR" andExpr)* ;
andExpr      -> predicate ("AND" predicate)* ;
predicate    -> IDENT state ;
action       -> IDENT state signedNumber? ;
signedNumber -> ("+" | "-") NUMBER ;
```

## Example script

```text
LOAD CATALOG FROM "data/hgnc_interleukins.csv";
LOAD NETWORK FROM "data/th17_network.csv";
LOAD RULES FROM "rules/th17_core.icirc";

SET IL6 = HIGH;
SET IL23A = HIGH;
SET TGFB1 = MODERATE;

RUN 3 STEPS;
EXPLAIN Th17;
```

## Example rule

```text
RULE th17_differentiation:
IF STAT3 ACTIVE AND TGFB1 MODERATE THEN RORC ACTIVE +2;
```

## Current grammar limitations

The first version intentionally omits:

- parentheses,
- NOT expressions,
- arithmetic expressions,
- user-defined functions.

These are good extension points because they would require meaningful parser and interpreter changes.
