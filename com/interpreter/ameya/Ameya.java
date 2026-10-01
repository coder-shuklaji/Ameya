package com.interpreter.ameya;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.interpreter.ameya.lexer.Scanner;
import com.interpreter.ameya.lexer.Token;
import com.interpreter.ameya.lexer.TokenType;
import com.interpreter.ameya.parser.*;

public class Ameya {
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    public static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage : ameya [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError)
            System.exit(65);
        if (hadRuntimeError)
            System.exit(70);
    }

    private static void runPrompt() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            for (;;) {
                System.out.print(">> ");
                String line = reader.readLine();
                if (line == null)
                    break;
                run(line);
                System.out.println();
                hadError = false;
            }
        }
    }

    private static void run(String source) {
        Scanner sc = new Scanner(source);
        List<Token> tokens = sc.scanTokens();

        Parser parser = new Parser(tokens);
        // Expr expression = parser.parse();
        List<Stmt> statements = parser.parse();

        if (hadError)
            return;
        interpreter.interpret(statements);
    }

    public static void error(int line, String message) {
        report(line, " ", message);
    }

    public static void error(Token token, String message) {
        if (token.type == TokenType.EOF)
            report(token.line, " at End ", message);
        else
            report(token.line, " at '" + token.lexeme + "'", message);
    }

    public static void runtimeError(RuntimeError error) {
        if (error.token == null) {
            System.err.println("Runtime Error: " + error.getMessage());
        } else {
            System.err.println("[line " + error.token.line + "] Runtime Error: " + error.getMessage());
        }
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}