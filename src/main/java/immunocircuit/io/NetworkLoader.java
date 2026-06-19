package immunocircuit.io;

import immunocircuit.network.ImmuneNetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads directed immune-network relationships from CSV.
 */
public class NetworkLoader {
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
                if (fields.size() < 4) {
                    throw new IOException("Network row must have 4 fields: " + line);
                }
                String source = fields.get(0);
                String target = fields.get(1);
                String relationship = fields.get(2);
                int weight = Integer.parseInt(fields.get(3).trim());
                network.addEdge(source, target, relationship, weight);
                count++;
            }
        }
        return count;
    }
}
