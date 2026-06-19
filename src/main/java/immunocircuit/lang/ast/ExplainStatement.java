package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;

public class ExplainStatement implements Statement {
    private final String symbol;

    public ExplainStatement(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public void execute(Interpreter interpreter) {
        interpreter.explain(symbol);
    }
}
