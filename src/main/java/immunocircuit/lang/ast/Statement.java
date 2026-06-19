package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;

public interface Statement {
    void execute(Interpreter interpreter);
}
