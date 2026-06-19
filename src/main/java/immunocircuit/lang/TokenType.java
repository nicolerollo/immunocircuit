package immunocircuit.lang;

public enum TokenType {
    // Single-character tokens
    EQUAL,
    SEMICOLON,
    COLON,
    PLUS,
    MINUS,

    // Literals
    IDENTIFIER,
    STRING,
    NUMBER,

    // Keywords
    LOAD,
    CATALOG,
    NETWORK,
    RULES,
    FROM,
    SET,
    RUN,
    STEPS,
    EXPLAIN,
    RANK,
    NODES,
    BY,
    SCORE,
    DESC,
    ASC,
    LIMIT,
    FIND,
    FEEDBACK_LOOPS,
    REPORT,
    UNMODELED_GENES,
    RULE,
    IF,
    THEN,
    AND,
    OR,

    EOF
}
