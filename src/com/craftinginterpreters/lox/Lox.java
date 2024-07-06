package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static boolean hadError;
    private static boolean hadRuntimeError;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String source = new String(bytes, Charset.defaultCharset());
        run(source);
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        Reader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        System.out.println("Welcome to Lox v0.1");
        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expr = parser.parse();
        interpreter.interpret(expr);
        System.out.println(new AstPrinter().print(expr));
    }

    public static void error(int line, String message) {
        reportError(message, "", line);
    }

    private static void reportError(String message, String where, int line) {
        System.err.printf("%n" + where + ": [line: " + line + "] Error" + ": " + message + "%n");
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            reportError(message, " at end-of-file", token.line);
        } else {
            reportError(message, " at '" + token.lexeme + "'", token.line);
        }
    }

    public static void runtimeError(RuntimeError e) {
        System.err.println(e.getMessage() + " [line:" + e.token.line + "]");
        hadRuntimeError = true;
    }
}
