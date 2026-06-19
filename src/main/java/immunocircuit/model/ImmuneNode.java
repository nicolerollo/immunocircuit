package immunocircuit.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a node in the synthetic immune network.
 *
 * A node can be a cytokine, receptor, signal mediator, or abstract cell program.
 * Nodes have a state, a score, aliases, and a flag indicating whether they are
 * actively modeled in the current scenario.
 */
public class ImmuneNode {
    private final String symbol;
    private String name;
    private NodeType type;
    private final List<String> aliases;
    private SignalState state;
    private int score;
    private boolean catalogMember;
    private boolean modeled;

    public ImmuneNode(String symbol, String name, NodeType type) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Node symbol cannot be blank.");
        }
        this.symbol = symbol.trim();
        this.name = name == null || name.isBlank() ? symbol.trim() : name.trim();
        this.type = type == null ? NodeType.OTHER : type;
        this.aliases = new ArrayList<>();
        this.state = SignalState.ABSENT;
        this.score = 0;
        this.catalogMember = false;
        this.modeled = false;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        if (type != null) {
            this.type = type;
        }
    }

    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    public void addAlias(String alias) {
        if (alias != null && !alias.isBlank()) {
            aliases.add(alias.trim());
        }
    }

    public SignalState getState() {
        return state;
    }

    public void setState(SignalState state) {
        this.state = state == null ? SignalState.ABSENT : state;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int delta) {
        this.score += delta;
    }

    public boolean isCatalogMember() {
        return catalogMember;
    }

    public void setCatalogMember(boolean catalogMember) {
        this.catalogMember = catalogMember;
    }

    public boolean isModeled() {
        return modeled;
    }

    public void setModeled(boolean modeled) {
        this.modeled = modeled;
    }

    @Override
    public String toString() {
        return symbol + " [" + type + ", state=" + state + ", score=" + score + "]";
    }
}
