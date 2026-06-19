package immunocircuit.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Small CSV helper for this portfolio project.
 * Supports quoted fields but intentionally avoids external dependencies.
 */
final class CsvSupport {
    private CsvSupport() {
    }

    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }
}
