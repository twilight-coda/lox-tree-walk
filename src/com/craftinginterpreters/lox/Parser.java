package com.craftinginterpreters.lox;

import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Expr expression() {
        return equality();
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
        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
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
            if (advance().type != TokenType.RIGHT_PAREN) {
                throw new Error("Matching ) not found");
            }
            return expr;
        }
        throw new Error("No matching primary token found");
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
}
