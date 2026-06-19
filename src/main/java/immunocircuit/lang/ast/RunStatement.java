package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;

public class RunStatement implements Statement {
    private final int steps;

    public RunStatement(int steps) {
        this.steps = steps;
    }

    @Override
    public void execute(Interpreter interpreter) {
        interpreter.runSteps(steps);
    }
}
