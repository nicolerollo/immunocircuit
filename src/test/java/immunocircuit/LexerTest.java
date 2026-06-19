package immunocircuit;

import immunocircuit.lang.Lexer;
import immunocircuit.lang.LexerException;
import immunocircuit.lang.Token;
import immunocircuit.lang.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {
    @Test
    void lexesSetStatement() {
        Lexer lexer = new Lexer("SET IL6 = HIGH;");
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.SET, tokens.get(0).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals("IL6", tokens.get(1).getLexeme());
        assertEquals(TokenType.EQUAL, tokens.get(2).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType());
        assertEquals(TokenType.SEMICOLON, tokens.get(4).getType());
    }

    @Test
    void recognizesKeywordsCaseInsensitively() {
        Lexer lexer = new Lexer("set IL6 = high;");
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.SET, tokens.get(0).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType());
        assertEquals("high", tokens.get(3).getLexeme());
    }

    @Test
    void rejectsUnexpectedCharacter() {
        Lexer lexer = new Lexer("SET IL6 = HIGH@;");

        LexerException ex = assertThrows(LexerException.class, lexer::scanTokens);
        assertTrue(ex.getMessage().contains("Unexpected character"));
    }

    @Test
    void rejectsUnterminatedString() {
        Lexer lexer = new Lexer("LOAD CATALOG FROM \"data/hgnc_interleukins.csv");

        LexerException ex = assertThrows(LexerException.class, lexer::scanTokens);
        assertTrue(ex.getMessage().contains("Unterminated string"));
    }
}
