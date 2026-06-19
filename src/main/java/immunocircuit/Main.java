package immunocircuit;

import immunocircuit.lang.Interpreter;
import immunocircuit.lang.Lexer;
import immunocircuit.lang.Parser;
import immunocircuit.lang.ast.Statement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point for ImmunoCircuit.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -cp out immunocircuit.Main <script.icirc>");
            System.exit(1);
        }

        Path script = Path.of(args[0]);
        try {
            String source = Files.readString(script);
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.scanTokens());
            List<Statement> statements = parser.parse();
            Interpreter interpreter = new Interpreter();
            String output = interpreter.interpret(statements);
            System.out.print(output);
        } catch (IOException ex) {
            System.err.println("Could not read script: " + ex.getMessage());
            System.exit(2);
        } catch (RuntimeException ex) {
            System.err.println(ex.getMessage());
            System.exit(3);
        }
    }
}
