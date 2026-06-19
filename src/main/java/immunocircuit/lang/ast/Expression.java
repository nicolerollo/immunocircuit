package immunocircuit.lang.ast;

import immunocircuit.network.ImmuneNetwork;

import java.util.Set;

/**
 * Rule-condition expression. Implementations form a small AST: a leaf
 * {@link PredicateExpression} or an internal {@link BinaryExpression} node.
 */
public interface Expression {
    boolean evaluate(ImmuneNetwork network);
    String describe();

    /**
     * Symbols this expression reads, collected by walking the expression
     * tree. Used by semantic analysis to confirm every predicate refers to
     * a node the program actually knows about.
     */
    Set<String> referencedSymbols();

    /**
     * Explains why this expression evaluated to {@code false} against
     * {@code network}, by naming the specific predicate(s) that failed
     * rather than just restating the whole condition. Behavior is
     * unspecified if the expression actually evaluated to {@code true} —
     * callers should only call this after confirming {@link #evaluate}
     * returned {@code false}.
     */
    String explainFalse(ImmuneNetwork network);
}
