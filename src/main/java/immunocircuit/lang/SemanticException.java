package immunocircuit.lang;

/**
 * Raised when a statement is syntactically valid but refers to something
 * that cannot be resolved against the current program state — for example
 * an unknown node symbol or a duplicate rule name. Kept distinct from
 * {@link ParserException} (syntax) and {@link InterpreterException}
 * (execution failure) so the three error classes map onto the three
 * phases of the pipeline: parse, resolve, execute.
 */
public class SemanticException extends RuntimeException {
    public SemanticException(String message) {
        super(message);
    }
}
