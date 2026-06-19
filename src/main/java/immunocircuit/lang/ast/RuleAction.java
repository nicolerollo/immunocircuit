package immunocircuit.lang.ast;

import immunocircuit.model.SignalState;

public class RuleAction {
    private final String target;
    private final SignalState state;
    private final int scoreDelta;

    public RuleAction(String target, SignalState state, int scoreDelta) {
        this.target = target;
        this.state = state;
        this.scoreDelta = scoreDelta;
    }

    public String getTarget() {
        return target;
    }

    public SignalState getState() {
        return state;
    }

    public int getScoreDelta() {
        return scoreDelta;
    }
}
