package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
    private final String source;
    private int current;
    private int start;
    private int line;
    private ArrayList<Token> tokens;
    private static final HashMap<String, TokenType> reservedKeywords;
    static {
        reservedKeywords = new HashMap<>();
        reservedKeywords.put("and", AND);
        reservedKeywords.put("class", CLASS);
        reservedKeywords.put("else", ELSE);
        reservedKeywords.put("false", FALSE);
        reservedKeywords.put("for", FOR);
        reservedKeywords.put("fun", FUN);
        reservedKeywords.put("if", IF);
        reservedKeywords.put("nil", NIL);
        reservedKeywords.put("or", OR);
        reservedKeywords.put("print", PRINT);
        reservedKeywords.put("return", RETURN);
        reservedKeywords.put("super", SUPER);
        reservedKeywords.put("this", THIS);
        reservedKeywords.put("true", TRUE);
        reservedKeywords.put("var", VAR);
        reservedKeywords.put("while", WHILE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token("", TokenType.EOF, line, null));
        return tokens;
    }

    private void scanToken() {
        char c = source.charAt(current++);
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!':
                addToken(matchNextChar('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(matchNextChar('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(matchNextChar('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(matchNextChar('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (matchNextChar('/')) {
                    while (!isAtEnd() && peek() != '\n') current ++;
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlphaOrUnderscore(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                    break;
                }
        }
    }

    private void identifier() {
        while (isAlphanumeric(source.charAt(current))) current++;
        String txt = source.substring(start, current);
        TokenType type = reservedKeywords.get(txt);
        if (type == null) {
            type = IDENTIFIER;
        }
        addToken(type);
    }

    private void addToken(TokenType tokenType) {
        addToken(tokenType, null);
    }

    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(lexeme, type, line, literal));
    }

    /**
     * The current lexeme started with a digit, which means this is a number.
     * Parse the source until digits are found, then check if a decimal point is
     * present. If so, consume it and iterate further on the source until the last digit.
     * Create a decimal number parsed from the substring.
     * NOTE: All numbers in Lox are double-precision floating-point numbers.
     */
    private void number() {
        while (isDigit(peek())) current++;
        if (peek() == '.' && isDigit(peekNext())) {
            do current++;
            while (isDigit(peek()));
        }

        Double lit = Double.parseDouble(source.substring(start, current));
        addToken(NUMBER, lit);
    }

    private boolean isDigit(char current) {
        return current >= '0' && current <= '9';
    }

    /**
     * An opening double quote has been encountered.
     * Advance the `current` index until the closing is found.
     * Multi-line strings are possible, so if a line ending is encountered increment the `line` index.
     * Finally, when the closing quote is found, create the string token with the literal value.
     */
    private void string() {
        while (!isAtEnd() && peek() != '"') {
            if (source.charAt(current) == '\n') line++;
            current++;
        }
        if (isAtEnd()) {
            Lox.error(line, "The string did not terminate properly");
            return;
        }

        String lit = source.substring(start + 1, current);
        current ++;
        addToken(STRING, lit);
    }

    /** `current` contains the index of the currently unconsumed character.
     * This method returns the char at this index but doesn't increment the `current` value.
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current >= source.length() - 1) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean matchNextChar(char expected) {
        if (current >= source.length()) return false;
        if (source.charAt(current) != expected) return true;
        current++;
        return true;
    }

    private boolean isAlphaOrUnderscore(char c) {
        return c >= 'a' && c <= 'b' ||
                c >= 'A' && c <= 'B' ||
                c == '_';
    }
    private boolean isAlphanumeric(char c) {
        return isAlphaOrUnderscore(c) || isDigit(c);
    }
}
