package immunocircuit;

import immunocircuit.lang.Interpreter;
import immunocircuit.lang.Lexer;
import immunocircuit.lang.Parser;
import immunocircuit.lang.SemanticException;
import immunocircuit.lang.ast.Statement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpreterTest {
    private static List<Statement> parse(String source) {
        Parser parser = new Parser(new Lexer(source).scanTokens());
        return parser.parse();
    }

    @Test
    void executesSmallRuleProgram() {
        String source = String.join("\n",
                "RULE demo:",
                "IF IL6 HIGH THEN STAT3 ACTIVE +2;",
                "SET IL6 = HIGH;",
                "RUN 1 STEPS;",
                "EXPLAIN STAT3;"
        );

        Interpreter interpreter = new Interpreter();
        String output = interpreter.interpret(parse(source));

        assertTrue(output.contains("STAT3"));
        assertTrue(output.contains("Score: 2"));
    }

    @Test
    void rejectsSetOnUnknownSymbolOnceRealDataIsLoaded() {
        // Once a catalog/network is loaded there is ground truth to typo
        // against, so SET must reference a real node. "IL6X" is a typo for
        // "IL6" and is not in either loaded file.
        String source = String.join("\n",
                "LOAD CATALOG FROM \"data/hgnc_interleukins.csv\";",
                "LOAD NETWORK FROM \"data/th17_network.csv\";",
                "SET IL6X = HIGH;"
        );

        SemanticException ex = assertThrows(SemanticException.class,
                () -> new Interpreter().interpret(parse(source)));
        assertTrue(ex.getMessage().contains("IL6X"));
    }

    @Test
    void allowsSetToDeclareASyntheticNodeWhenNoDataIsLoaded() {
        // No LOAD at all means there is nothing to typo against, so SET
        // may introduce a node directly. This is what lets a rule be
        // exercised in isolation, as executesSmallRuleProgram() does.
        String source = "SET CUSTOM_NODE = HIGH;";

        Interpreter interpreter = new Interpreter();
        String output = interpreter.interpret(parse(source));

        assertTrue(output.contains("Set CUSTOM_NODE = HIGH"));
    }

    @Test
    void rejectsRuleWithUnresolvedPredicateSymbol() {
        String source = String.join("\n",
                "RULE demo:",
                "IF GHOSTGENE HIGH THEN STAT3 ACTIVE +2;",
                "RUN 1 STEPS;"
        );

        SemanticException ex = assertThrows(SemanticException.class,
                () -> new Interpreter().interpret(parse(source)));
        assertTrue(ex.getMessage().contains("GHOSTGENE"));
    }

    @Test
    void rejectsDuplicateRuleNames() {
        String source = String.join("\n",
                "RULE demo:",
                "IF IL6 HIGH THEN STAT3 ACTIVE +1;",
                "RULE demo:",
                "IF IL6 LOW THEN STAT3 ACTIVE +1;",
                "SET IL6 = HIGH;",
                "RUN 1 STEPS;"
        );

        SemanticException ex = assertThrows(SemanticException.class,
                () -> new Interpreter().interpret(parse(source)));
        assertTrue(ex.getMessage().contains("Duplicate rule name"));
    }

    @Test
    void ruleTargetIsADeclarationSiteNotARequiredSymbol() {
        // STAT3 is never LOADed or SET — it only ever appears as a rule
        // target — so it must be accepted as a predicate symbol in a later
        // rule without the analyzer rejecting it as unknown.
        String source = String.join("\n",
                "RULE step1:",
                "IF IL6 HIGH THEN STAT3 ACTIVE +2;",
                "RULE step2:",
                "IF STAT3 ACTIVE THEN RORC ACTIVE +1;",
                "SET IL6 = HIGH;",
                "RUN 1 STEPS;",
                "EXPLAIN RORC;"
        );

        Interpreter interpreter = new Interpreter();
        String output = interpreter.interpret(parse(source));

        assertTrue(output.contains("Score: 1"));
    }

    @Test
    void caseInsensitiveSymbolsResolveAcrossRuleAndSet() {
        // The lexer, SignalState.fromString, and ImmuneNetwork's internal
        // map keys are all case-insensitive; SemanticAnalyzer must agree,
        // or "il6" and "IL6" would be treated as two different symbols.
        String source = String.join("\n",
                "RULE demo:",
                "IF il6 HIGH THEN STAT3 ACTIVE +1;",
                "SET IL6 = HIGH;",
                "RUN 1 STEPS;",
                "EXPLAIN STAT3;"
        );

        Interpreter interpreter = new Interpreter();
        String output = interpreter.interpret(parse(source));

        assertTrue(output.contains("Score: 1"));
    }

    @Test
    void setResolvesCatalogAliasToCanonicalSymbol() {
        // C17orf99's catalog row lists "IL40|IL-40|interleukin 40" as
        // aliases. SET IL40 must mutate the same node as SET C17orf99,
        // not silently create a second, disconnected "IL40" node.
        String source = String.join("\n",
                "LOAD CATALOG FROM \"data/hgnc_interleukins.csv\";",
                "SET IL40 = HIGH;",
                "EXPLAIN C17orf99;"
        );

        Interpreter interpreter = new Interpreter();
        String output = interpreter.interpret(parse(source));

        assertTrue(output.contains("Explanation for C17orf99"));
        assertTrue(output.contains("State: HIGH"));
    }

    @Test
    void aliasResolvesInsideRulePredicateNotJustSetAndExplain() {
        // Regression test: SemanticAnalyzer.validateRules() used to check
        // predicate symbols against a pre-collected set of canonical
        // node.getSymbol() values, which never contains aliases - so
        // "IF IL40 HIGH" was rejected even though SET IL40 = HIGH; and
        // EXPLAIN IL40; both resolved fine. It must check
        // network.findNode() (alias-aware) instead.
        String source = String.join("\n",
                "LOAD CATALOG FROM \"data/hgnc_interleukins.csv\";",
                "RULE alias_rule:",
                "IF IL40 HIGH THEN STAT3 ACTIVE +1;",
                "SET IL40 = HIGH;",
                "RUN 1 STEPS;",
                "EXPLAIN STAT3;"
        );

        Interpreter interpreter = new Interpreter();
        String output = interpreter.interpret(parse(source));

        assertTrue(output.contains("Score: 1"));
    }

    @Test
    void networkLoadDoesNotOverwriteCatalogName() {
        String source = String.join("\n",
                "LOAD CATALOG FROM \"data/hgnc_interleukins.csv\";",
                "LOAD NETWORK FROM \"data/th17_network.csv\";",
                "EXPLAIN IL6;"
        );

        Interpreter interpreter = new Interpreter();
        String output = interpreter.interpret(parse(source));

        assertTrue(output.contains("Name: interleukin 6"));
    }

    @Test
    void explainRejectsUnknownSymbolOnceRealDataIsLoaded() {
        String source = String.join("\n",
                "LOAD CATALOG FROM \"data/hgnc_interleukins.csv\";",
                "EXPLAIN IL6X;"
        );

        SemanticException ex = assertThrows(SemanticException.class,
                () -> new Interpreter().interpret(parse(source)));
        assertTrue(ex.getMessage().contains("IL6X"));
    }

    @Test
    void explainReportsBothFiredAndBlockedRules() {
        String source = String.join("\n",
                "RULE fires:",
                "IF IL6 HIGH THEN STAT3 ACTIVE +2;",
                "RULE blocked:",
                "IF IL10 HIGH THEN STAT3 SUPPRESSED -2;",
                "SET IL6 = HIGH;",
                "SET IL10 = LOW;",
                "RUN 1 STEPS;",
                "EXPLAIN STAT3;"
        );

        Interpreter interpreter = new Interpreter();
        String output = interpreter.interpret(parse(source));

        assertTrue(output.contains("Rules fired:"));
        assertTrue(output.contains("fires: IL6 was HIGH -> STAT3 ACTIVE +2"));
        assertTrue(output.contains("Rules blocked:"));
        assertTrue(output.contains("blocked: IL10 was LOW, expected HIGH"));
    }
}
