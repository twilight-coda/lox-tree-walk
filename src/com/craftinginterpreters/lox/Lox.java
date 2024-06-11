package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Lox {
    private static boolean hadError;

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
        }
    }

    private static void run(String source) {
        System.out.println("running..");
        if (hadError) System.exit(65);
    }

    public static void error(int line, String message) {
        reportError(message, "", line);
    }

    private static void reportError(String message, String fileName, int line) {
        System.err.printf("%n" + fileName + ": [line: " + line + "] Error" + ": " + message + "%n");
        hadError = true;
    }
}
