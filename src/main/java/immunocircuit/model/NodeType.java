package immunocircuit.model;

/**
 * Broad category for a node in the immune network.
 */
public enum NodeType {
    CYTOKINE,
    RECEPTOR,
    SIGNAL,
    CELL_PROGRAM,
    OTHER;

    public static NodeType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        try {
            return NodeType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}
