package immunocircuit.lang;

import immunocircuit.lang.ast.BinaryExpression;
import immunocircuit.lang.ast.ExplainStatement;
import immunocircuit.lang.ast.Expression;
import immunocircuit.lang.ast.FindFeedbackLoopsStatement;
import immunocircuit.lang.ast.LoadStatement;
import immunocircuit.lang.ast.PredicateExpression;
import immunocircuit.lang.ast.RankStatement;
import immunocircuit.lang.ast.ReportUnmodeledGenesStatement;
import immunocircuit.lang.ast.RuleAction;
import immunocircuit.lang.ast.RuleStatement;
import immunocircuit.lang.ast.RunStatement;
import immunocircuit.lang.ast.SetStatement;
import immunocircuit.lang.ast.Statement;
import immunocircuit.model.SignalState;

import java.util.ArrayList;
import java.util.List;

/**
 * Handwritten recursive-descent parser for ICirc Script.
 */
public class Parser {
    private final List<Token> tokens;
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }

    public List<Statement> parse() {
        List<Statement> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }
        return statements;
    }

    private Statement statement() {
        if (match(TokenType.LOAD)) {
            return loadStatement();
        }
        if (match(TokenType.SET)) {
            return setStatement();
        }
        if (match(TokenType.RUN)) {
            return runStatement();
        }
        if (match(TokenType.EXPLAIN)) {
            return explainStatement();
        }
        if (match(TokenType.RANK)) {
            return rankStatement();
        }
        if (match(TokenType.FIND)) {
            return findStatement();
        }
        if (match(TokenType.REPORT)) {
            return reportStatement();
        }
        if (match(TokenType.RULE)) {
            return ruleStatement();
        }
        throw error(peek(), "Expected a statement.");
    }

    private Statement loadStatement() {
        LoadStatement.LoadKind kind;
        if (match(TokenType.CATALOG)) {
            kind = LoadStatement.LoadKind.CATALOG;
        } else if (match(TokenType.NETWORK)) {
            kind = LoadStatement.LoadKind.NETWORK;
        } else if (match(TokenType.RULES)) {
            kind = LoadStatement.LoadKind.RULES;
        } else {
            throw error(peek(), "Expected CATALOG, NETWORK, or RULES after LOAD.");
        }
        consume(TokenType.FROM, "Expected FROM in LOAD statement.");
        String path = consume(TokenType.STRING, "Expected a quoted path.").getLexeme();
        consume(TokenType.SEMICOLON, "Expected ';' after LOAD statement.");
        return new LoadStatement(kind, path);
    }

    private Statement setStatement() {
        String symbol = consume(TokenType.IDENTIFIER, "Expected symbol after SET.").getLexeme();
        consume(TokenType.EQUAL, "Expected '=' after symbol.");
        SignalState state = parseState(consume(TokenType.IDENTIFIER, "Expected state after '='."));
        consume(TokenType.SEMICOLON, "Expected ';' after SET statement.");
        return new SetStatement(symbol, state);
    }

    private Statement runStatement() {
        int steps = parseNumber(consume(TokenType.NUMBER, "Expected number of steps."));
        consume(TokenType.STEPS, "Expected STEPS after number.");
        consume(TokenType.SEMICOLON, "Expected ';' after RUN statement.");
        return new RunStatement(steps);
    }

    private Statement explainStatement() {
        String symbol = consume(TokenType.IDENTIFIER, "Expected symbol after EXPLAIN.").getLexeme();
        consume(TokenType.SEMICOLON, "Expected ';' after EXPLAIN statement.");
        return new ExplainStatement(symbol);
    }

    private Statement rankStatement() {
        consume(TokenType.NODES, "Expected NODES after RANK.");
        consume(TokenType.BY, "Expected BY after NODES.");
        consume(TokenType.SCORE, "Expected SCORE after BY.");
        boolean descending = true;
        if (match(TokenType.ASC)) {
            descending = false;
        } else if (match(TokenType.DESC)) {
            descending = true;
        }
        consume(TokenType.LIMIT, "Expected LIMIT in RANK statement.");
        int limit = parseNumber(consume(TokenType.NUMBER, "Expected numeric limit."));
        consume(TokenType.SEMICOLON, "Expected ';' after RANK statement.");
        return new RankStatement(descending, limit);
    }

    private Statement findStatement() {
        consume(TokenType.FEEDBACK_LOOPS, "Expected FEEDBACK_LOOPS after FIND.");
        consume(TokenType.SEMICOLON, "Expected ';' after FIND statement.");
        return new FindFeedbackLoopsStatement();
    }

    private Statement reportStatement() {
        consume(TokenType.UNMODELED_GENES, "Expected UNMODELED_GENES after REPORT.");
        consume(TokenType.SEMICOLON, "Expected ';' after REPORT statement.");
        return new ReportUnmodeledGenesStatement();
    }

    private Statement ruleStatement() {
        String name = consume(TokenType.IDENTIFIER, "Expected rule name.").getLexeme();
        consume(TokenType.COLON, "Expected ':' after rule name.");
        consume(TokenType.IF, "Expected IF after rule name.");
        Expression condition = condition();
        consume(TokenType.THEN, "Expected THEN after rule condition.");
        RuleAction action = action();
        consume(TokenType.SEMICOLON, "Expected ';' after rule.");
        return new RuleStatement(name, condition, action);
    }

    private Expression condition() {
        return orExpression();
    }

    private Expression orExpression() {
        Expression expression = andExpression();
        while (match(TokenType.OR)) {
            Expression right = andExpression();
            expression = new BinaryExpression(expression, BinaryExpression.Operator.OR, right);
        }
        return expression;
    }

    private Expression andExpression() {
        Expression expression = predicate();
        while (match(TokenType.AND)) {
            Expression right = predicate();
            expression = new BinaryExpression(expression, BinaryExpression.Operator.AND, right);
        }
        return expression;
    }

    private Expression predicate() {
        String symbol = consume(TokenType.IDENTIFIER, "Expected symbol in predicate.").getLexeme();
        SignalState state = parseState(consume(TokenType.IDENTIFIER, "Expected state in predicate."));
        return new PredicateExpression(symbol, state);
    }

    private RuleAction action() {
        String target = consume(TokenType.IDENTIFIER, "Expected target symbol in action.").getLexeme();
        SignalState state = parseState(consume(TokenType.IDENTIFIER, "Expected state in action."));
        int delta = 0;
        if (match(TokenType.PLUS)) {
            delta = parseNumber(consume(TokenType.NUMBER, "Expected number after '+'."));
        } else if (match(TokenType.MINUS)) {
            delta = -parseNumber(consume(TokenType.NUMBER, "Expected number after '-'."));
        }
        return new RuleAction(target, state, delta);
    }

    private SignalState parseState(Token token) {
        try {
            return SignalState.fromString(token.getLexeme());
        } catch (IllegalArgumentException ex) {
            throw error(token, "Unknown state '" + token.getLexeme() + "'.");
        }
    }

    private int parseNumber(Token token) {
        try {
            return Integer.parseInt(token.getLexeme());
        } catch (NumberFormatException ex) {
            throw error(token, "Expected integer number.");
        }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParserException error(Token token, String message) {
        return new ParserException("Parse error at line " + token.getLine() + ", column " + token.getColumn() + ": " + message);
    }
}
