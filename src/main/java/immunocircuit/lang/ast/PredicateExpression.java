package immunocircuit.lang.ast;

import immunocircuit.model.SignalState;
import immunocircuit.network.ImmuneNetwork;

import java.util.Set;

public class PredicateExpression implements Expression {
    private final String symbol;
    private final SignalState expected;

    public PredicateExpression(String symbol, SignalState expected) {
        this.symbol = symbol;
        this.expected = expected;
    }

    public String getSymbol() {
        return symbol;
    }

    public SignalState getExpected() {
        return expected;
    }

    @Override
    public boolean evaluate(ImmuneNetwork network) {
        return network.getState(symbol) == expected;
    }

    @Override
    public String describe() {
        return symbol + " was " + expected;
    }

    @Override
    public Set<String> referencedSymbols() {
        return Set.of(symbol);
    }

    @Override
    public String explainFalse(ImmuneNetwork network) {
        return symbol + " was " + network.getState(symbol) + ", expected " + expected;
    }
}
