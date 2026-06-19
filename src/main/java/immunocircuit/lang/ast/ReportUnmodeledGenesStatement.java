package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;

public class ReportUnmodeledGenesStatement implements Statement {
    @Override
    public void execute(Interpreter interpreter) {
        interpreter.reportUnmodeledGenes();
    }
}
