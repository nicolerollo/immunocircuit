# Complexity Analysis

This document summarizes the major data structures and algorithmic costs in ImmunoCircuit.

Let:

- `V` = number of nodes in the immune network,
- `E` = number of directed edges,
- `R` = number of rules,
- `S` = number of simulation steps,
- `T` = number of tokens in a script.

## Symbol lookup

Nodes are stored in a `HashMap<String, ImmuneNode>`.

- Average lookup: `O(1)`
- Worst-case lookup: `O(V)` if many keys collide, though Java's implementation mitigates this.

This is better than scanning a list of genes for every rule evaluation.

## Graph storage

Edges are stored as an adjacency list:

```text
Map<String, List<InfluenceEdge>> adjacency
```

Space complexity:

```text
O(V + E)
```

## Rule evaluation

Each simulation step evaluates each rule once.

For a rule with a small condition tree, evaluation is approximately constant. More generally, if `C` is the number of predicates in a condition:

```text
O(C)
```

For the whole simulation:

```text
O(S * R * C)
```

Because the starter rules are short, the practical cost is close to:

```text
O(S * R)
```

## Ranking nodes

`ImmuneNetwork.rankByScore()` uses Java's `Stream.sorted()` over the full node list, then truncates to the requested limit.

```text
O(V log V)
```

This is simple and correct, but it sorts every node even when the caller only wants the top 10 out of, say, 43. A heap-based top-k (a bounded `PriorityQueue` of size `k`, push every node, evict the worst) would be:

```text
O(V log k)
```

which matters once `V` is large relative to `k`. Left as a documented tradeoff rather than implemented, since for this project's `V` (tens of nodes) the full sort is not a real bottleneck — the point is recognizing *when* the bound would change, not micro-optimizing a small `V`.

## Feedback-loop detection

`ImmuneNetwork.findFeedbackLoops()` runs `dfsForCycles()` from every node, tracking the current path in a `LinkedHashSet` (so "is this node already on the path" is `O(1)` instead of `O(path length)`).

```text
O(V + E)
```

per traversal, run once per starting node, so the implemented version is closer to `O(V * (V + E))` than the single-traversal `O(V + E)` that `docs/architecture.md` cites — that figure is the per-start-node cost, not the whole-network cost. Even `O(V * (V + E))` understates the true worst case, though: this is exhaustive simple-cycle enumeration, not single-source cycle *detection*. A graph can have exponentially many simple cycles in the number of nodes (a dense or highly-connected graph), and the DFS explores a path for each one before backtracking, so the real worst-case cost is exponential, not polynomial. For this project's small, sparse scenario graph (a few dozen nodes, a handful of feedback paths) that worst case never shows up in practice — but the honest statement is "practical for this graph's size and density," not "polynomial in general." Each discovered cycle is also canonicalized (`canonicalCycleKey()` rotates the cycle to start at its lexicographically smallest node) before being added to a `Set`, so rotations of the same cycle are only reported once — without this, a 3-node cycle would be found and reported up to 3 times (once per starting node).

## Semantic analysis

`SemanticAnalyzer.validateRules()` builds a symbol table from `V` nodes plus `R` rule targets — `O(V + R)` — then, for every rule, walks its condition tree to collect referenced symbols and checks each against the table. For `C` total predicates across all rules, this is:

```text
O(V + R + C)
```

run once per `RUN` statement, not once per simulation step — it is a static check, not a per-step cost.

## Lexing

The lexer reads each character in the source file once.

```text
O(number of characters)
```

## Parsing

The recursive-descent parser consumes each token in order.

```text
O(T)
```

## Interpretation

Interpreting command statements is mostly linear in the number of statements, except for commands that invoke specific algorithms such as ranking or feedback-loop detection.

## Design takeaway

The project uses data structures intentionally:

| Need | Data structure | Reason |
|---|---|---|
| Fast symbol validation | HashMap | Average constant-time lookup |
| Pathway relationships | Adjacency list | Efficient graph storage |
| Rule logic | AST | Separates syntax from execution |
| Explanation | List of traces | Preserves interpreter history |
| Ranking | Sorted list | Simple and readable for portfolio version |
