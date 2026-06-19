package immunocircuit.lang.ast;

import immunocircuit.network.ImmuneNetwork;

import java.util.HashSet;
import java.util.Set;

public class BinaryExpression implements Expression {
    public enum Operator {
        AND,
        OR
    }

    private final Expression left;
    private final Operator operator;
    private final Expression right;

    public BinaryExpression(Expression left, Operator operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public boolean evaluate(ImmuneNetwork network) {
        return switch (operator) {
            case AND -> left.evaluate(network) && right.evaluate(network);
            case OR -> left.evaluate(network) || right.evaluate(network);
        };
    }

    @Override
    public String describe() {
        return "(" + left.describe() + " " + operator + " " + right.describe() + ")";
    }

    @Override
    public Set<String> referencedSymbols() {
        Set<String> symbols = new HashSet<>(left.referencedSymbols());
        symbols.addAll(right.referencedSymbols());
        return symbols;
    }

    @Override
    public String explainFalse(ImmuneNetwork network) {
        // AND is false because at least one side failed - only report the
        // side(s) that actually failed, not a side that was satisfied.
        // OR is false only when both sides failed, so both are reported.
        boolean leftFailed = !left.evaluate(network);
        boolean rightFailed = !right.evaluate(network);
        if (operator == Operator.AND) {
            if (leftFailed && rightFailed) {
                return left.explainFalse(network) + " AND " + right.explainFalse(network);
            }
            return leftFailed ? left.explainFalse(network) : right.explainFalse(network);
        }
        return left.explainFalse(network) + " OR " + right.explainFalse(network);
    }
}
