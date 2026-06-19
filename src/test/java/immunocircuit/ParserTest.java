package immunocircuit;

import immunocircuit.lang.Lexer;
import immunocircuit.lang.Parser;
import immunocircuit.lang.ParserException;
import immunocircuit.lang.ast.RuleStatement;
import immunocircuit.lang.ast.Statement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {
    @Test
    void parsesRuleStatement() {
        String source = "RULE demo: IF IL6 HIGH AND TGFB1 MODERATE THEN RORC ACTIVE +2;";
        Parser parser = new Parser(new Lexer(source).scanTokens());
        List<Statement> statements = parser.parse();

        assertEquals(1, statements.size());
        assertInstanceOf(RuleStatement.class, statements.get(0));
    }

    @Test
    void parsesOrConditionWithLowerPrecedenceThanAnd() {
        // IF A AND B OR C must parse as (A AND B) OR C, mirroring AND > OR precedence.
        String source = "RULE demo: IF IL1B HIGH AND TNF HIGH OR IL6 HIGH THEN NFkB ACTIVE +1;";
        Parser parser = new Parser(new Lexer(source).scanTokens());
        RuleStatement rule = (RuleStatement) parser.parse().get(0);

        assertEquals("((IL1B was HIGH AND TNF was HIGH) OR IL6 was HIGH)", rule.getCondition().describe());
    }

    @Test
    void rejectsSetStatementMissingEquals() {
        Parser parser = new Parser(new Lexer("SET IL6 HIGH;").scanTokens());

        ParserException ex = assertThrows(ParserException.class, parser::parse);
        assertTrue(ex.getMessage().contains("Expected '='"));
    }

    @Test
    void rejectsRuleMissingThen() {
        Parser parser = new Parser(new Lexer("RULE demo: IF IL6 HIGH RORC ACTIVE +2;").scanTokens());

        ParserException ex = assertThrows(ParserException.class, parser::parse);
        assertTrue(ex.getMessage().contains("Expected THEN"));
    }
}
