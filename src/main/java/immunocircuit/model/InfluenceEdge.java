package immunocircuit.model;

/**
 * Directed relationship between two immune-network nodes.
 */
public class InfluenceEdge {
    private final String source;
    private final String target;
    private final String relationship;
    private final int weight;

    public InfluenceEdge(String source, String target, String relationship, int weight) {
        if (source == null || source.isBlank() || target == null || target.isBlank()) {
            throw new IllegalArgumentException("Edge source and target are required.");
        }
        this.source = source.trim();
        this.target = target.trim();
        this.relationship = relationship == null || relationship.isBlank() ? "influences" : relationship.trim();
        this.weight = weight;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getRelationship() {
        return relationship;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return source + " -[" + relationship + ":" + weight + "]-> " + target;
    }
}
