package immunocircuit.model;

/**
 * Simplified state values used by the synthetic immune circuit model.
 *
 * <p>The seven values mix two different kinds of fact about a node, and
 * the language deliberately does not enforce which kind applies to which
 * node — that is left to the rule author, the same way an untyped scripting
 * language lets you compare a string to a number if you really want to.
 *
 * <ul>
 *   <li><b>Level states</b> ({@link #ABSENT}, {@link #LOW}, {@link #MODERATE},
 *   {@link #HIGH}) describe a measured quantity, e.g. a cytokine's
 *   concentration.</li>
 *   <li><b>Condition states</b> ({@link #PRESENT}, {@link #ACTIVE},
 *   {@link #SUPPRESSED}) describe a qualitative on/off-ish condition, e.g.
 *   a receptor being bound or a transcription factor being engaged.</li>
 * </ul>
 *
 * <p>A stricter dialect could split this into two enums and have the
 * semantic analyzer reject combinations like {@code IF IL6 ACTIVE} (a
 * cytokine is never "active," only present in some amount). This project
 * keeps them unified because the rule set in {@code rules/th17_core.icirc}
 * intentionally drives a cytokine's level down via {@code SUPPRESSED}
 * (see {@code regulatory_brake}) — collapsing that distinction would
 * require redesigning the existing rules, not just the enum.
 */
public enum SignalState {
    // Level states: a measured quantity.
    ABSENT,
    LOW,
    MODERATE,
    HIGH,
    // Condition states: a qualitative on/off-ish state.
    PRESENT,
    ACTIVE,
    SUPPRESSED;

    public static SignalState fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Signal state cannot be blank.");
        }
        return SignalState.valueOf(raw.trim().toUpperCase());
    }
}
