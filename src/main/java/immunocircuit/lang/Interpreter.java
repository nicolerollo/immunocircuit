package immunocircuit.lang;

import immunocircuit.io.CatalogLoader;
import immunocircuit.io.NetworkLoader;
import immunocircuit.lang.ast.LoadStatement;
import immunocircuit.lang.ast.RuleStatement;
import immunocircuit.lang.ast.Statement;
import immunocircuit.model.ImmuneNode;
import immunocircuit.model.SignalState;
import immunocircuit.network.ImmuneNetwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Executes ICirc AST statements over an immune-network graph.
 */
public class Interpreter {
    private final ImmuneNetwork network;
    private final List<RuleStatement> rules;
    private final StringBuilder output;

    public Interpreter() {
        this.network = new ImmuneNetwork();
        this.rules = new ArrayList<>();
        this.output = new StringBuilder();
    }

    public String interpret(List<Statement> statements) {
        for (Statement statement : statements) {
            statement.execute(this);
        }
        return output.toString();
    }

    public ImmuneNetwork getNetwork() {
        return network;
    }

    public void load(LoadStatement.LoadKind kind, String rawPath) {
        Path path = Path.of(rawPath);
        try {
            switch (kind) {
                case CATALOG -> {
                    int count = new CatalogLoader().load(path, network);
                    println("Loaded catalog: " + rawPath + " (" + count + " rows)");
                }
                case NETWORK -> {
                    int count = new NetworkLoader().load(path, network);
                    println("Loaded network: " + rawPath + " (" + count + " edges)");
                }
                case RULES -> {
                    loadRules(path);
                    println("Loaded rules: " + rawPath + " (" + rules.size() + " total rules)");
                }
            }
        } catch (IOException ex) {
            throw new InterpreterException("Could not load " + rawPath + ": " + ex.getMessage(), ex);
        }
    }

    public void setState(String symbol, SignalState state) {
        SemanticAnalyzer.validateSetTarget(network, symbol);
        network.setState(symbol, state);
        println("Set " + symbol + " = " + state);
    }

    public void addRule(RuleStatement rule) {
        rules.add(rule);
    }

    public void runSteps(int steps) {
        if (steps < 0) {
            throw new InterpreterException("Step count cannot be negative.");
        }
        SemanticAnalyzer.validateRules(network, rules);
        for (int step = 1; step <= steps; step++) {
            for (RuleStatement rule : rules) {
                if (rule.getCondition().evaluate(network)) {
                    network.applyRuleAction(
                            step,
                            rule.getName(),
                            rule.getAction().getTarget(),
                            rule.getAction().getState(),
                            rule.getAction().getScoreDelta(),
                            rule.getCondition().describe()
                    );
                } else {
                    network.recordBlocked(step, rule.getName(), rule.getAction().getTarget(),
                            rule.getCondition().explainFalse(network));
                }
            }
        }
        println("Ran cascade for " + steps + " step(s).\n");
    }

    public void explain(String symbol) {
        SemanticAnalyzer.validateExplainTarget(network, symbol);
        println(network.explain(symbol));
    }

    public void rankNodes(boolean descending, int limit) {
        println("Ranked nodes by score " + (descending ? "DESC" : "ASC") + " limit " + limit);
        List<ImmuneNode> ranked = network.rankByScore(descending, limit);
        for (int i = 0; i < ranked.size(); i++) {
            ImmuneNode node = ranked.get(i);
            println((i + 1) + ". " + node.getSymbol() + " | state=" + node.getState() + " | score=" + node.getScore() + " | type=" + node.getType());
        }
        println("");
    }

    public void findFeedbackLoops() {
        List<List<String>> cycles = network.findFeedbackLoops();
        if (cycles.isEmpty()) {
            println("Feedback loops: none found\n");
            return;
        }
        println("Feedback loops found: " + cycles.size());
        for (List<String> cycle : cycles) {
            println("- " + String.join(" -> ", cycle));
        }
        println("");
    }

    public void reportUnmodeledGenes() {
        println(network.unmodeledGeneReport());
    }

    private void loadRules(Path path) throws IOException {
        String source = Files.readString(path);
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer.scanTokens());
        List<Statement> statements = parser.parse();
        for (Statement statement : statements) {
            if (!(statement instanceof RuleStatement)) {
                throw new InterpreterException("Rule files may only contain RULE statements: " + path);
            }
            statement.execute(this);
        }
    }

    private void println(String text) {
        output.append(text).append(System.lineSeparator());
    }
}
