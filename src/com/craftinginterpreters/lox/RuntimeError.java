package com.craftinginterpreters.lox;

public class RuntimeError extends RuntimeException {
    final Token token;
    private final String message;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
