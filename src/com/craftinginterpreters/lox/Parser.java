package com.craftinginterpreters.lox;

import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return ternary();
    }

    private Expr ternary() {
        Expr expr = commaSeparated();
        if (match(TokenType.QUESTION)) {
            Token opOne = previous();
            Expr then = commaSeparated();
            consume(TokenType.COLON, "Ternary operator requires an else condition followed by a ':'");
            Token opTwo = previous();
            Expr otherwise = commaSeparated();
            expr = new Expr.Ternary(expr, opOne, then, opTwo, otherwise);
        }
        return expr;
    }

    private Expr commaSeparated() {
        Expr expr = equality();
        while (match(TokenType.COMMA)) {
            Token op = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (
                match(
                        TokenType.LESS,
                        TokenType.LESS_EQUAL,
                        TokenType.GREATER,
                        TokenType.GREATER_EQUAL
                )
        ) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            return new Expr.Unary(operator, unary());
        } else if (
                match(
                        TokenType.EQUAL_EQUAL,
                        TokenType.BANG_EQUAL,
                        TokenType.LESS,
                        TokenType.LESS_EQUAL,
                        TokenType.GREATER,
                        TokenType.GREATER_EQUAL,
                        TokenType.PLUS,
                        TokenType.STAR,
                        TokenType.SLASH
                )
        ) {
            throw error(peek(), "Expect an operand before '" + peek().lexeme + "'");
        }
        return primary();
    }

    private Expr primary() {
        if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal);
        if (match(TokenType.NIL)) return new Expr.Literal(null);
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = new Expr.Grouping(expression());
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return expr;
        }
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String s) {
        if (check(type)) return advance();
        throw error(peek(), s);
    }

    boolean match(TokenType... tokens) {
        for (TokenType type : tokens) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType token) {
        if (isAtEnd()) return false;
        return peek().type == token;
    }

    Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    Token peek() {
        return this.tokens.get(current);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        Token prev = advance();
        if (prev.type == TokenType.SEMICOLON) return;

        while (!isAtEnd()) {
            switch (peek().type) {
                case IF:
                case FUN:
                case CLASS:
                case VAR:
                case RETURN:
                case FOR:
                case WHILE:
                case PRINT:
                    return;
            }
            advance();
        }
    }
}
