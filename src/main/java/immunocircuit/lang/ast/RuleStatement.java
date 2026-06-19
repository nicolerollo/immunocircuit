package immunocircuit.lang.ast;

import immunocircuit.lang.Interpreter;

public class RuleStatement implements Statement {
    private final String name;
    private final Expression condition;
    private final RuleAction action;

    public RuleStatement(String name, Expression condition, RuleAction action) {
        this.name = name;
        this.condition = condition;
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public Expression getCondition() {
        return condition;
    }

    public RuleAction getAction() {
        return action;
    }

    @Override
    public void execute(Interpreter interpreter) {
        interpreter.addRule(this);
    }
}
