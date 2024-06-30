package com.craftinginterpreters.lox;

public class Token {
    final String lexeme;
    final TokenType type;
    final int line;
    final Object literal;

    public Token(String lexeme, TokenType type, int line, Object literal) {
        this.lexeme = lexeme;
        this.type = type;
        this.line = line;
        this.literal = literal;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal + " line: " + line;
    }
}
