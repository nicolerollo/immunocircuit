package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;

public class RankStatement implements Statement {
    private final boolean descending;
    private final int limit;

    public RankStatement(boolean descending, int limit) {
        this.descending = descending;
        this.limit = limit;
    }

    @Override
    public void execute(Interpreter interpreter) {
        interpreter.rankNodes(descending, limit);
    }
}
