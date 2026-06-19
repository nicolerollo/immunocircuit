package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;

public class LoadStatement implements Statement {
    public enum LoadKind {
        CATALOG,
        NETWORK,
        RULES
    }

    private final LoadKind kind;
    private final String path;

    public LoadStatement(LoadKind kind, String path) {
        this.kind = kind;
        this.path = path;
    }

    public LoadKind getKind() {
        return kind;
    }

    public String getPath() {
        return path;
    }

    @Override
    public void execute(Interpreter interpreter) {
        interpreter.load(kind, path);
    }
}
