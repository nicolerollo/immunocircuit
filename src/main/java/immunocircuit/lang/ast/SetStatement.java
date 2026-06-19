package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;
import immunocircuit.model.SignalState;

public class SetStatement implements Statement {
    private final String symbol;
    private final SignalState state;

    public SetStatement(String symbol, SignalState state) {
        this.symbol = symbol;
        this.state = state;
    }

    public String getSymbol() {
        return symbol;
    }

    public SignalState getState() {
        return state;
    }

    @Override
    public void execute(Interpreter interpreter) {
        interpreter.setState(symbol, state);
    }
}
