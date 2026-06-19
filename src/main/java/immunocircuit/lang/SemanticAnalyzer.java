package immunocircuit.lang;

import immunocircuit.lang.ast.RuleStatement;
import immunocircuit.network.ImmuneNetwork;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves symbols after parsing and before execution. This is the
 * "semantic" phase of the pipeline: parsing only checks that a script is
 * grammatically well-formed (e.g. {@code SET IDENT = state ;}), it does
 * not know whether {@code IDENT} refers to anything real. The analyzer
 * checks that every symbol a script reads or writes is resolvable against
 * a symbol table built from the loaded catalog/network plus every symbol
 * a rule action declares, and that rule names are unique.
 *
 * <p>A rule action target counts as a declaration site because the rule
 * language lets a script introduce synthetic signal nodes (e.g. {@code
 * NFkB}, {@code RORC}) that never appear in the HGNC catalog or the
 * network CSV. A predicate or {@code SET} target, by contrast, is a use
 * site and must resolve to something already known.
 */
public final class SemanticAnalyzer {

    private SemanticAnalyzer() {
    }

    /**
     * Validates a {@code SET} target against nodes already known to the
     * network (catalog members and network-loaded nodes). Unlike rule
     * targets, {@code SET} has no declaration semantics once real data is
     * loaded — it assigns an initial condition to an existing node, so an
     * unresolved symbol is almost always a typo. If no catalog or network
     * has been loaded at all, there is no ground truth to check against,
     * so {@code SET} is allowed to declare a synthetic node directly —
     * this is what lets a script (or a test) exercise a rule in isolation
     * without LOADing any CSV.
     */
    public static void validateSetTarget(ImmuneNetwork network, String symbol) {
        validateKnownSymbol(network, symbol, "SET");
    }

    /**
     * Validates an {@code EXPLAIN} target the same way as a {@code SET}
     * target: once real data is loaded, asking to explain a symbol that
     * resolves to nothing is a semantic error, not just an empty report.
     * Before this check existed, {@code EXPLAIN IL6X;} printed a harmless
     * "No node found" message — fine for a casual tool, but it hid a
     * genuine use-site error that the language pipeline should catch the
     * same way it catches a bad {@code SET}.
     */
    public static void validateExplainTarget(ImmuneNetwork network, String symbol) {
        validateKnownSymbol(network, symbol, "EXPLAIN");
    }

    private static void validateKnownSymbol(ImmuneNetwork network, String symbol, String statementKeyword) {
        if (network.hasLoadedData() && network.findNode(symbol).isEmpty()) {
            throw new SemanticException(
                    "Unknown symbol '" + symbol + "' in " + statementKeyword + " statement. "
                            + "It is not in the loaded catalog or network - check for a typo, "
                            + "or LOAD the data that defines it before " + statementKeyword + ".");
        }
    }

    /**
     * Validates a full rule set against the network's symbol table:
     * - no two rules share a name,
     * - every predicate symbol resolves to a known node or to some rule's
     *   action target (a forward reference, since rules may be declared
     *   in any order).
     */
    public static void validateRules(ImmuneNetwork network, List<RuleStatement> rules) {
        // A predicate symbol is known if either:
        //  (a) the network already resolves it - directly, or through the
        //      catalog's alias table (network.findNode() checks both), or
        //  (b) some rule's action target declares it (a forward reference,
        //      since rules may be declared in any order).
        // Checking against network.findNode() rather than a pre-collected
        // set of canonical symbols is what makes this alias-aware: a set
        // built from node.getSymbol() values would only ever contain
        // canonical symbols, so "IF IL40 HIGH" would be rejected even
        // though SET IL40 = HIGH; and EXPLAIN IL40; both resolve it fine.
        Set<String> actionTargets = new HashSet<>();
        for (RuleStatement rule : rules) {
            actionTargets.add(normalize(rule.getAction().getTarget()));
        }

        Set<String> seenNames = new HashSet<>();
        for (RuleStatement rule : rules) {
            if (!seenNames.add(normalize(rule.getName()))) {
                throw new SemanticException("Duplicate rule name '" + rule.getName() + "'.");
            }
        }

        for (RuleStatement rule : rules) {
            for (String symbol : rule.getCondition().referencedSymbols()) {
                boolean knownNode = network.findNode(symbol).isPresent();
                boolean declaredByRuleTarget = actionTargets.contains(normalize(symbol));
                if (!knownNode && !declaredByRuleTarget) {
                    throw new SemanticException(
                            "Rule '" + rule.getName() + "' references unknown symbol '" + symbol
                                    + "'. It must be in the loaded catalog/network (or a known alias) or be some rule's action target.");
                }
            }
        }
    }

    private static String normalize(String symbol) {
        return symbol.trim().toUpperCase();
    }
}
