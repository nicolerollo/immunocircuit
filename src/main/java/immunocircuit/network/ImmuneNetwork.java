package immunocircuit.network;

import immunocircuit.model.ImmuneNode;
import immunocircuit.model.InfluenceEdge;
import immunocircuit.model.NodeType;
import immunocircuit.model.RuleTrace;
import immunocircuit.model.SignalState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Directed immune-network graph backed by a HashMap and adjacency list.
 */
public class ImmuneNetwork {
    private final Map<String, ImmuneNode> nodes;
    private final Map<String, List<InfluenceEdge>> adjacency;
    private final Map<String, List<RuleTrace>> tracesByTarget;
    private final Map<String, List<String>> blockedTracesByTarget;
    private final Map<String, String> aliasToCanonical;

    public ImmuneNetwork() {
        this.nodes = new LinkedHashMap<>();
        this.adjacency = new HashMap<>();
        this.tracesByTarget = new HashMap<>();
        this.blockedTracesByTarget = new HashMap<>();
        this.aliasToCanonical = new HashMap<>();
    }

    /**
     * Registers {@code alias} (e.g. {@code IL40}, {@code interleukin 40}) as
     * another name for {@code canonicalSymbol} (e.g. {@code C17orf99}).
     * Called by {@code CatalogLoader} while loading the catalog's alias
     * column. {@link #findNode} checks the direct symbol first and falls
     * back to this map, so {@code SET IL40 = HIGH;} resolves to the same
     * node as {@code SET C17orf99 = HIGH;}.
     */
    public void registerAlias(String alias, String canonicalSymbol) {
        if (alias == null || alias.isBlank() || canonicalSymbol == null || canonicalSymbol.isBlank()) {
            return;
        }
        aliasToCanonical.put(normalize(alias), normalize(canonicalSymbol));
    }

    /**
     * Whether any real data (a catalog or a network CSV) has been loaded.
     * Used to decide how strictly {@code SET} should be checked: with no
     * external data loaded there is no ground truth to typo against, so a
     * script that only declares synthetic nodes through {@code SET} and
     * {@code RULE} targets (a common way to unit-test a rule in isolation)
     * is legitimate. Once real data is loaded, {@code SET} must reference
     * a real node — see {@link immunocircuit.lang.SemanticAnalyzer}.
     */
    public boolean hasLoadedData() {
        if (!adjacency.values().stream().allMatch(List::isEmpty)) {
            return true;
        }
        return nodes.values().stream().anyMatch(ImmuneNode::isCatalogMember);
    }

    public ImmuneNode getOrCreateNode(String symbol, String name, NodeType type) {
        String key = normalize(symbol);
        ImmuneNode existing = nodes.get(key);
        if (existing != null) {
            // A node's name is only ever upgraded, never downgraded: once
            // something has given it a real name (e.g. the catalog's
            // "interleukin 6"), a later caller that merely repeats the bare
            // symbol (e.g. NetworkLoader, which has no name data of its own
            // and passes the symbol as the name) must not clobber it.
            boolean stillUnnamed = existing.getName().equals(existing.getSymbol());
            boolean newNameIsMeaningful = name != null && !name.equals(symbol);
            if (stillUnnamed && newNameIsMeaningful) {
                existing.setName(name);
            }
            // Same logic for type: a catalog row's curated NodeType (e.g.
            // CYTOKINE from the HGNC data) is authoritative and must not be
            // replaced by a later caller's rougher inference (e.g.
            // NetworkLoader.inferType(), which only pattern-matches on the
            // symbol string). Once a node isn't a catalog member, though,
            // there's no curated type to protect, so later callers may
            // still refine it.
            if (!existing.isCatalogMember()) {
                existing.setType(type);
            }
            return existing;
        }
        ImmuneNode created = new ImmuneNode(symbol, name, type);
        nodes.put(key, created);
        adjacency.putIfAbsent(created.getSymbol(), new ArrayList<>());
        return created;
    }

    /**
     * Resolves a symbol to a node, trying the symbol itself first and then
     * falling back to the alias table built from the catalog's alias
     * column. This is the only place alias resolution happens — every
     * other method (SET, predicates, EXPLAIN) goes through this.
     */
    public Optional<ImmuneNode> findNode(String symbol) {
        String key = normalize(symbol);
        ImmuneNode direct = nodes.get(key);
        if (direct != null) {
            return Optional.of(direct);
        }
        String canonical = aliasToCanonical.get(key);
        return canonical == null ? Optional.empty() : Optional.ofNullable(nodes.get(canonical));
    }

    public ImmuneNode requireNode(String symbol) {
        return findNode(symbol).orElseThrow(() -> new IllegalArgumentException("Unknown node: " + symbol));
    }

    public void addEdge(String source, String target, String relationship, int weight) {
        // Alias-aware, like setState/applyRuleAction below: if the network
        // CSV ever referenced a node by an alias instead of its canonical
        // symbol, this resolves to the existing catalog node instead of
        // creating a second, disconnected one.
        ImmuneNode sourceNode = resolveOrCreate(source);
        ImmuneNode targetNode = resolveOrCreate(target);
        sourceNode.setModeled(true);
        targetNode.setModeled(true);
        adjacency.computeIfAbsent(sourceNode.getSymbol(), ignored -> new ArrayList<>())
                .add(new InfluenceEdge(sourceNode.getSymbol(), targetNode.getSymbol(), relationship, weight));
    }

    public void setState(String symbol, SignalState state) {
        ImmuneNode node = resolveOrCreate(symbol);
        node.setState(state);
        node.setModeled(true);
    }

    public SignalState getState(String symbol) {
        return findNode(symbol).map(ImmuneNode::getState).orElse(SignalState.ABSENT);
    }

    public void applyRuleAction(int step, String ruleName, String target, SignalState state, int delta, String conditionSummary) {
        ImmuneNode node = resolveOrCreate(target);
        node.setState(state);
        node.addScore(delta);
        node.setModeled(true);
        String message = conditionSummary + " -> " + node.getSymbol() + " " + state + " " + signed(delta);
        tracesByTarget.computeIfAbsent(node.getSymbol(), ignored -> new ArrayList<>())
                .add(new RuleTrace(step, ruleName, node.getSymbol(), message));
    }

    /**
     * Resolves an alias to its canonical node if one exists; otherwise
     * creates a new node under the given symbol. Used by {@code SET} and
     * rule actions so that {@code SET IL40 = HIGH;} finds the same node as
     * {@code SET C17orf99 = HIGH;} instead of creating a duplicate "IL40"
     * node alongside the catalog's real one.
     */
    private ImmuneNode resolveOrCreate(String symbol) {
        return findNode(symbol).orElseGet(() -> getOrCreateNode(symbol, symbol, inferType(symbol)));
    }

    public void recordBlocked(int step, String ruleName, String target, String reason) {
        ImmuneNode node = findNode(target).orElse(null);
        String key = node != null ? node.getSymbol() : normalize(target);
        String message = "step " + step + ": " + ruleName + ": " + reason;
        blockedTracesByTarget.computeIfAbsent(key, ignored -> new ArrayList<>()).add(message);
    }

    public List<String> getBlockedTracesFor(String symbol) {
        Optional<ImmuneNode> node = findNode(symbol);
        String key = node.map(ImmuneNode::getSymbol).orElse(normalize(symbol));
        return blockedTracesByTarget.getOrDefault(key, List.of());
    }

    public List<ImmuneNode> getNodes() {
        return new ArrayList<>(nodes.values());
    }

    public List<InfluenceEdge> getEdgesFrom(String source) {
        return adjacency.getOrDefault(source, List.of());
    }

    public List<ImmuneNode> rankByScore(boolean descending, int limit) {
        Comparator<ImmuneNode> comparator = Comparator.comparingInt(ImmuneNode::getScore);
        if (descending) {
            comparator = comparator.reversed();
        }
        return nodes.values().stream()
                .sorted(comparator.thenComparing(ImmuneNode::getSymbol))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<RuleTrace> getTracesFor(String symbol) {
        return tracesByTarget.getOrDefault(symbol, List.of());
    }

    public String explain(String symbol) {
        Optional<ImmuneNode> maybeNode = findNode(symbol);
        if (maybeNode.isEmpty()) {
            return "No node found for " + symbol;
        }
        ImmuneNode node = maybeNode.get();
        StringBuilder builder = new StringBuilder();
        builder.append("Explanation for ").append(node.getSymbol()).append(System.lineSeparator());
        builder.append("Name: ").append(node.getName()).append(System.lineSeparator());
        builder.append("Type: ").append(node.getType()).append(System.lineSeparator());
        builder.append("State: ").append(node.getState()).append(System.lineSeparator());
        builder.append("Score: ").append(node.getScore()).append(System.lineSeparator());
        List<RuleTrace> traces = getTracesFor(node.getSymbol());
        if (traces.isEmpty()) {
            builder.append("Rules fired: none").append(System.lineSeparator());
        } else {
            builder.append("Rules fired:").append(System.lineSeparator());
            for (RuleTrace trace : traces) {
                builder.append("  ").append(trace).append(System.lineSeparator());
            }
        }
        List<String> blocked = getBlockedTracesFor(node.getSymbol());
        if (blocked.isEmpty()) {
            builder.append("Rules blocked: none").append(System.lineSeparator());
        } else {
            builder.append("Rules blocked:").append(System.lineSeparator());
            for (String entry : blocked) {
                builder.append("  ").append(entry).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    public String unmodeledGeneReport() {
        long catalogCount = nodes.values().stream().filter(ImmuneNode::isCatalogMember).count();
        List<ImmuneNode> unmodeled = nodes.values().stream()
                .filter(ImmuneNode::isCatalogMember)
                .filter(node -> !node.isModeled())
                .collect(Collectors.toList());
        long modeledCatalog = catalogCount - unmodeled.size();

        StringBuilder builder = new StringBuilder();
        builder.append("Catalog report").append(System.lineSeparator());
        builder.append("Loaded catalog genes: ").append(catalogCount).append(System.lineSeparator());
        builder.append("Modeled catalog genes in current scenario: ").append(modeledCatalog).append(System.lineSeparator());
        builder.append("Catalog-only genes: ").append(unmodeled.size()).append(System.lineSeparator());
        if (!unmodeled.isEmpty()) {
            builder.append("Unmodeled symbols: ");
            builder.append(unmodeled.stream().map(ImmuneNode::getSymbol).collect(Collectors.joining(", ")));
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    public List<List<String>> findFeedbackLoops() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> seenCycleKeys = new HashSet<>();
        for (String node : adjacency.keySet()) {
            dfsForCycles(node, node, new LinkedHashSet<>(), cycles, seenCycleKeys);
        }
        return cycles;
    }

    private void dfsForCycles(String start, String current, LinkedHashSet<String> path,
                              List<List<String>> cycles, Set<String> seenCycleKeys) {
        if (path.contains(current)) {
            return;
        }
        path.add(current);
        for (InfluenceEdge edge : adjacency.getOrDefault(current, List.of())) {
            String next = edge.getTarget();
            if (next.equals(start) && path.size() > 1) {
                List<String> cycle = new ArrayList<>(path);
                cycle.add(start);
                String key = canonicalCycleKey(cycle);
                if (seenCycleKeys.add(key)) {
                    cycles.add(cycle);
                }
            } else if (!path.contains(next)) {
                dfsForCycles(start, next, path, cycles, seenCycleKeys);
            }
        }
        path.remove(current);
    }

    private String canonicalCycleKey(List<String> cycle) {
        if (cycle.size() <= 1) {
            return String.join("->", cycle);
        }
        List<String> body = new ArrayList<>(cycle.subList(0, cycle.size() - 1));
        String smallest = body.stream().min(String::compareTo).orElse(body.get(0));
        Deque<String> deque = new ArrayDeque<>(body);
        while (!deque.peekFirst().equals(smallest)) {
            deque.addLast(deque.removeFirst());
        }
        return String.join("->", deque);
    }

    private NodeType inferType(String symbol) {
        if (symbol == null) {
            return NodeType.OTHER;
        }
        String s = symbol.toUpperCase();
        if (s.equals("IL6R") || s.equals("IL23R") || s.equals("IL17RA") || s.equals("IL17RC") || s.equals("TNFRSF1A")) {
            return NodeType.RECEPTOR;
        }
        if (s.startsWith("IL") || s.equals("TNF") || s.equals("IFNG") || s.equals("TGFB1")) {
            return NodeType.CYTOKINE;
        }
        if (s.equals("TH17") || s.equals("TREG")) {
            return NodeType.CELL_PROGRAM;
        }
        return NodeType.SIGNAL;
    }

    private String normalize(String symbol) {
        return symbol.trim().toUpperCase();
    }

    private String signed(int delta) {
        return delta >= 0 ? "+" + delta : Integer.toString(delta);
    }
}
