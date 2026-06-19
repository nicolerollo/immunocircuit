package immunocircuit;

import immunocircuit.model.ImmuneNode;
import immunocircuit.model.NodeType;
import immunocircuit.model.SignalState;
import immunocircuit.network.ImmuneNetwork;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkTest {
    @Test
    void detectsSimpleFeedbackLoop() {
        ImmuneNetwork network = new ImmuneNetwork();
        network.addEdge("A", "B", "activates", 1);
        network.addEdge("B", "C", "activates", 1);
        network.addEdge("C", "A", "activates", 1);

        List<List<String>> cycles = network.findFeedbackLoops();
        assertEquals(1, cycles.size(), "rotations of the same 3-cycle must be deduplicated");
    }

    @Test
    void reportsNoFeedbackLoopsForAcyclicGraph() {
        ImmuneNetwork network = new ImmuneNetwork();
        network.addEdge("A", "B", "activates", 1);
        network.addEdge("B", "C", "activates", 1);

        assertTrue(network.findFeedbackLoops().isEmpty());
    }

    @Test
    void ranksNodesByScoreDescendingWithSymbolTieBreak() {
        ImmuneNetwork network = new ImmuneNetwork();
        network.setState("B", SignalState.HIGH);
        network.setState("A", SignalState.HIGH);
        network.requireNode("A").addScore(5);
        network.requireNode("B").addScore(5);
        network.setState("C", SignalState.HIGH);
        network.requireNode("C").addScore(9);

        List<ImmuneNode> ranked = network.rankByScore(true, 10);

        assertEquals(List.of("C", "A", "B"), ranked.stream().map(ImmuneNode::getSymbol).toList());
    }

    @Test
    void rankLimitTruncatesResults() {
        ImmuneNetwork network = new ImmuneNetwork();
        network.setState("A", SignalState.HIGH);
        network.setState("B", SignalState.HIGH);
        network.setState("C", SignalState.HIGH);

        assertEquals(2, network.rankByScore(true, 2).size());
    }

    @Test
    void catalogMemberTypeSurvivesGetOrCreateNodeWithARoughInferredType() {
        // Regression test mirroring the name-downgrade fix: once a node is
        // a catalog member, its curated NodeType (here RECEPTOR) must not
        // be replaced by a later, rougher guess (here CYTOKINE) from a
        // caller like NetworkLoader that has no curated metadata of its
        // own. No gene in the bundled catalog actually exposes this (every
        // catalog row happens to agree with the naive symbol-pattern
        // guess), so this is exercised directly against ImmuneNetwork
        // instead of through a script.
        ImmuneNetwork network = new ImmuneNetwork();
        ImmuneNode node = network.getOrCreateNode("XYZ", "some receptor", NodeType.RECEPTOR);
        node.setCatalogMember(true);

        network.getOrCreateNode("XYZ", "XYZ", NodeType.CYTOKINE);

        assertEquals(NodeType.RECEPTOR, network.requireNode("XYZ").getType());
    }

    @Test
    void nonCatalogNodeTypeCanStillBeRefined() {
        // The protection above is specific to catalog members. A synthetic
        // node with no curated type (e.g. one introduced only by a rule
        // target) should still be free to have its type adjusted.
        ImmuneNetwork network = new ImmuneNetwork();
        network.getOrCreateNode("XYZ", "XYZ", NodeType.OTHER);

        network.getOrCreateNode("XYZ", "XYZ", NodeType.SIGNAL);

        assertEquals(NodeType.SIGNAL, network.requireNode("XYZ").getType());
    }
}
