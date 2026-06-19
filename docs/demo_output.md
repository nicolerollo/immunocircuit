# Demo Output

This is the verbatim output of:

```bash
./build.sh
./run.sh examples/th17_demo.icirc
```

captured against the current `rules/th17_core.icirc` and `data/th17_network.csv`. If you change either file, this output will change too — re-run the command above and update this file rather than hand-editing the numbers.

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

Explanation for RORC
Name: RORC
Type: SIGNAL
State: ACTIVE
Score: 6
Rules fired:
  step 1: th17_differentiation: (STAT3 was ACTIVE AND TGFB1 was MODERATE) -> RORC ACTIVE +2
  step 2: th17_differentiation: (STAT3 was ACTIVE AND TGFB1 was MODERATE) -> RORC ACTIVE +2
  step 3: th17_differentiation: (STAT3 was ACTIVE AND TGFB1 was MODERATE) -> RORC ACTIVE +2
Rules blocked: none

Explanation for NFkB
Name: NFkB
Type: SIGNAL
State: ACTIVE
Score: 12
Rules fired:
  step 1: inflammatory_push: (IL1B was HIGH OR TNF was HIGH) -> NFkB ACTIVE +2
  step 1: il36_amplification: (IL36A was HIGH OR IL36G was HIGH) -> NFkB ACTIVE +2
  step 2: inflammatory_push: (IL1B was HIGH OR TNF was HIGH) -> NFkB ACTIVE +2
  step 2: il36_amplification: (IL36A was HIGH OR IL36G was HIGH) -> NFkB ACTIVE +2
  step 3: inflammatory_push: (IL1B was HIGH OR TNF was HIGH) -> NFkB ACTIVE +2
  step 3: il36_amplification: (IL36A was HIGH OR IL36G was HIGH) -> NFkB ACTIVE +2
Rules blocked: none

Ranked nodes by score DESC limit 10
1. Th17 | state=ACTIVE | score=18 | type=CELL_PROGRAM
2. NFkB | state=ACTIVE | score=12 | type=SIGNAL
3. STAT3 | state=ACTIVE | score=12 | type=SIGNAL
4. RORC | state=ACTIVE | score=6 | type=SIGNAL
5. IL23R | state=PRESENT | score=3 | type=RECEPTOR
6. IL6 | state=HIGH | score=3 | type=CYTOKINE
7. C17orf99 | state=ABSENT | score=0 | type=CYTOKINE
8. FOXP3 | state=ABSENT | score=0 | type=SIGNAL
9. IL10 | state=LOW | score=0 | type=CYTOKINE
10. IL11 | state=ABSENT | score=0 | type=CYTOKINE

Feedback loops found: 1
- TNF -> NFkB -> TNF

Catalog report
Loaded catalog genes: 43
Modeled catalog genes in current scenario: 11
Catalog-only genes: 32
Unmodeled symbols: IL1A, IL1RN, IL2, IL3, IL4, IL5, IL7, IL9, IL11, IL12A, IL12B, IL13, IL15, IL16, IL17B, IL17C, IL17D, IL18, IL19, IL20, IL24, IL25, IL26, IL27, IL31, IL32, IL33, IL34, IL36B, IL36RN, IL38, C17orf99
```

## Reading the Th17 score

`Th17` ends at score 18, not the smaller number you might expect from eyeballing the rule file. Two separate rules target it:

```text
RULE th17_stabilization:
IF IL23A HIGH AND RORC ACTIVE THEN Th17 ACTIVE +3;

RULE th17_program:
IF RORC ACTIVE THEN Th17 ACTIVE +3;
```

Both conditions stay true for all 3 steps of the cascade, so both rules fire on every step: `(3 + 3) * 3 = 18`. There is no "already active, don't re-fire" guard and no per-step convergence check — a rule fires every step its condition holds, for as many steps as `RUN` asks for. This is intentional: it keeps the interpreter simple (no extra state to track per rule per step) and it is exactly the kind of cumulative, repeated-activation behavior real signaling cascades exhibit, even though the model itself is synthetic. See `docs/known_tradeoffs.md`.

## Reading "Rules blocked"

`Th17`'s explanation also lists a blocked rule: `treg_counterbalance` (`IF FOXP3 ACTIVE THEN Th17 SUPPRESSED -3;`) never fires in this scenario because `FOXP3` is never set to `ACTIVE` — it stays `ABSENT` for the whole run. `EXPLAIN` reports that explicitly (`FOXP3 was ABSENT, expected ACTIVE`) instead of silently omitting the rule, which is what makes the explanation two-sided: not just "what happened" but "what almost happened, and why it didn't."

## Reproducing this output

```bash
chmod +x build.sh run.sh
./build.sh
./run.sh examples/th17_demo.icirc
```

If your output differs from what's above, either the rule file, the network CSV, or the catalog has changed since this was captured — that's not a bug, just a sign this file needs to be refreshed.
