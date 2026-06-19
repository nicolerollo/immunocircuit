package immunocircuit.model;

/**
 * Explanation record created whenever a rule fires.
 */
public class RuleTrace {
    private final int step;
    private final String ruleName;
    private final String target;
    private final String message;

    public RuleTrace(int step, String ruleName, String target, String message) {
        this.step = step;
        this.ruleName = ruleName;
        this.target = target;
        this.message = message;
    }

    public int getStep() {
        return step;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getTarget() {
        return target;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "step " + step + ": " + ruleName + ": " + message;
    }
}
