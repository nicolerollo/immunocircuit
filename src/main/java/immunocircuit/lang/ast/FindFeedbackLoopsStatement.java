package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;

public class FindFeedbackLoopsStatement implements Statement {
    @Override
    public void execute(Interpreter interpreter) {
        interpreter.findFeedbackLoops();
    }
}
