package immunocircuit.io;

import immunocircuit.model.ImmuneNode;
import immunocircuit.model.NodeType;
import immunocircuit.network.ImmuneNetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads the controlled interleukin catalog.
 */
public class CatalogLoader {
    public int load(Path path, ImmuneNetwork network) throws IOException {
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = reader.readLine();
            if (header == null) {
                return 0;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = CsvSupport.parseLine(line);
                if (fields.size() < 5) {
                    throw new IOException("Catalog row must have 5 fields: " + line);
                }
                String symbol = fields.get(0);
                String name = fields.get(1);
                NodeType type = NodeType.fromString(fields.get(2));
                String aliases = fields.get(3);
                boolean modeled = Boolean.parseBoolean(fields.get(4));

                ImmuneNode node = network.getOrCreateNode(symbol, name, type);
                node.setCatalogMember(true);
                node.setModeled(modeled);
                for (String alias : aliases.split("\\|")) {
                    node.addAlias(alias);
                    network.registerAlias(alias, node.getSymbol());
                }
                count++;
            }
        }
        return count;
    }
}
