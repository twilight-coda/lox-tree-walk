package com.craftinginterpreters.lox;

public class Token {
    private final String lexeme;
    private final TokenType type;
    private final int line;
    private final Object literal;

    public Token(String lexeme, TokenType type, int line, Object literal) {
        this.lexeme = lexeme;
        this.type = type;
        this.line = line;
        this.literal = literal;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
