package immunocircuit.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts ICirc Script source text into tokens.
 */
public class Lexer {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("LOAD", TokenType.LOAD);
        KEYWORDS.put("CATALOG", TokenType.CATALOG);
        KEYWORDS.put("NETWORK", TokenType.NETWORK);
        KEYWORDS.put("RULES", TokenType.RULES);
        KEYWORDS.put("FROM", TokenType.FROM);
        KEYWORDS.put("SET", TokenType.SET);
        KEYWORDS.put("RUN", TokenType.RUN);
        KEYWORDS.put("STEPS", TokenType.STEPS);
        KEYWORDS.put("EXPLAIN", TokenType.EXPLAIN);
        KEYWORDS.put("RANK", TokenType.RANK);
        KEYWORDS.put("NODES", TokenType.NODES);
        KEYWORDS.put("BY", TokenType.BY);
        KEYWORDS.put("SCORE", TokenType.SCORE);
        KEYWORDS.put("DESC", TokenType.DESC);
        KEYWORDS.put("ASC", TokenType.ASC);
        KEYWORDS.put("LIMIT", TokenType.LIMIT);
        KEYWORDS.put("FIND", TokenType.FIND);
        KEYWORDS.put("FEEDBACK_LOOPS", TokenType.FEEDBACK_LOOPS);
        KEYWORDS.put("REPORT", TokenType.REPORT);
        KEYWORDS.put("UNMODELED_GENES", TokenType.UNMODELED_GENES);
        KEYWORDS.put("RULE", TokenType.RULE);
        KEYWORDS.put("IF", TokenType.IF);
        KEYWORDS.put("THEN", TokenType.THEN);
        KEYWORDS.put("AND", TokenType.AND);
        KEYWORDS.put("OR", TokenType.OR);
    }

    private final String source;
    private final List<Token> tokens;
    private int start;
    private int current;
    private int line;
    private int column;
    private int tokenColumn;

    public Lexer(String source) {
        this.source = source == null ? "" : source;
        this.tokens = new ArrayList<>();
        this.start = 0;
        this.current = 0;
        this.line = 1;
        this.column = 1;
        this.tokenColumn = 1;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            tokenColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '=' -> addToken(TokenType.EQUAL);
            case ';' -> addToken(TokenType.SEMICOLON);
            case ':' -> addToken(TokenType.COLON);
            case '+' -> addToken(TokenType.PLUS);
            case '-' -> addToken(TokenType.MINUS);
            case ' ', '\r', '\t' -> {
                // Ignore whitespace.
            }
            case '\n' -> {
                line++;
                column = 1;
            }
            case '#' -> skipComment();
            case '"' -> string();
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isIdentifierStart(c)) {
                    identifier();
                } else {
                    throw new LexerException("Unexpected character '" + c + "' at line " + line + ", column " + tokenColumn);
                }
            }
        }
    }

    private void identifier() {
        while (!isAtEnd() && isIdentifierPart(peek())) {
            advance();
        }
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text.toUpperCase(), TokenType.IDENTIFIER);
        addToken(type, text);
    }

    private void number() {
        while (!isAtEnd() && isDigit(peek())) {
            advance();
        }
        addToken(TokenType.NUMBER);
    }

    private void string() {
        StringBuilder builder = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\n') {
                line++;
                column = 1;
            }
            builder.append(c);
        }
        if (isAtEnd()) {
            throw new LexerException("Unterminated string at line " + line + ", column " + tokenColumn);
        }
        advance();
        addToken(TokenType.STRING, builder.toString());
    }

    private void skipComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char advance() {
        char c = source.charAt(current++);
        column++;
        return c;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void addToken(TokenType type) {
        addToken(type, source.substring(start, current));
    }

    private void addToken(TokenType type, String lexeme) {
        tokens.add(new Token(type, lexeme, line, tokenColumn));
    }
}
