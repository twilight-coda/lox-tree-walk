package com.craftinginterpreters.lox;

import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private boolean parsingCallArgs;

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.VAR)) {
                return varDecl();
            }
        } catch (ParseError e) {
            synchronize();
            return null;
        }
        return statement();
    }

    private Stmt varDecl() {
        Token varName = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(varName, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.FOR)) return forLoopStatement();
        if (match(TokenType.FUN)) return functionStatement("function");
        if (match(TokenType.RETURN)) return returnStmt();
        return expressionStatement();
    }

    private Stmt functionStatement(String kind) {
        Token fnName = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        ArrayList<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name"));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after " + kind + " name.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(fnName, parameters, body);
    }

    private Stmt returnStmt() {
        Token returnKeyword = previous();
        Expr returnExpression = check(TokenType.SEMICOLON) ? null : expression();
        consume(TokenType.SEMICOLON, "Expect ';' after return statement.");
        return new Stmt.Return(returnKeyword, returnExpression);
    }

    private Stmt forLoopStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDecl();
        } else {
            initializer = expressionStatement();
        }
        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after for loop initializer.");
        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = statement();
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
            ));
        }
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);
        if (initializer != null) body = new Stmt.Block(Arrays.asList( initializer, body ));
        return body;
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenStatements = statement();
        Stmt elseStatements = match(TokenType.ELSE) ? statement() : null;
        return new Stmt.If(condition, thenStatements, elseStatements);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition");
        Stmt whileBlock = statement();
        return new Stmt.While(condition, whileBlock);
    }

    private List<Stmt> block() {
        ArrayList<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(value);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = ternary();
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token var = ((Expr.Variable) expr).identifier;
                return new Expr.Assign(var, value);
            }
            error(equals, "invalid assignment target.");
        }
        return expr;
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
        Expr expr = or();
        if (parsingCallArgs) return expr;
        while (match(TokenType.COMMA)) {
            Token op = previous();
            Expr right = or();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();
        if (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        if (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
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
        return call();
    }

    private Expr call() {
        Expr callee = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                callee = finishCall(callee);
            } else {
                break;
            }
        }
        return callee;
    }

    private Expr finishCall(Expr callee) {
        ArrayList<Expr> args = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            parsingCallArgs = true;
            try {
                do {
                    if (args.size() >= 255) {
                        error(peek(), "Can't have more than 255 arguments");
                    }
                    args.add(expression());
                } while (match(TokenType.COMMA));
            } finally {
                parsingCallArgs = false;
            }
        }
        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after function arguments");
        callee = new Expr.Call(callee, paren, args);
        return callee;
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

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
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
