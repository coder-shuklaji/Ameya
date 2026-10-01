package com.interpreter.ameya.parser;

import com.interpreter.ameya.lexer.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.interpreter.ameya.Ameya;
import com.interpreter.ameya.lexer.Token;

public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private boolean match(TokenType... tokenTypes) {
        for (TokenType type : tokenTypes) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    private boolean checkAny(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = logical_or();

        if (match(TokenType.EQUAL, TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL,
                TokenType.STAR_EQUAL, TokenType.SLASH_EQUAL, TokenType.MOD_EQUAL,
                TokenType.LEFT_SHIFT_EQUAL, TokenType.RIGHT_SHIFT_EQUAL,
                TokenType.AND_EQUAL, TokenType.XOR_EQUAL, TokenType.OR_EQUAL,
            TokenType.INTEGER_DIV_EQUAL)) {

            Token operatorToken = previous();
            Expr value = assignment();

            if (!(expr instanceof Expr.Variable)) {
                throw error(operatorToken, "Invalid Assignment Operator.");
            }

            Token name = ((Expr.Variable) expr).name;

            if (operatorToken.type == TokenType.EQUAL) {
                return new Expr.Assign(name, value);
            }

            Token binaryOperator = compoundOperatorToken(operatorToken);

            Expr desugaredValue = new Expr.Binary(expr, binaryOperator, value);

            return new Expr.Assign(name, desugaredValue);
        }

        return expr;
    }

    private Expr logical_or() {
        Expr expr = logical_xor();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = logical_xor();

            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr logical_xor() {
        Expr expr = logical_and();

        while (match(TokenType.XOR)) {
            Token operator = previous();
            Expr right = logical_and();

            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr logical_and() {
        Expr expr = bitwise_or();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = bitwise_or();

            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr bitwise_or() {
        Expr expr = bitwise_xor();

        while (match(TokenType.BITWISE_OR)) {
            Token operator = previous();
            Expr right = bitwise_xor();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr bitwise_xor() {
        Expr expr = bitwise_and();

        while (match(TokenType.BITWISE_XOR)) {
            Token operator = previous();
            Expr right = bitwise_and();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr bitwise_and() {
        Expr expr = equality();

        while (match(TokenType.BITWISE_AND)) {
            Token operator = previous();
            Expr right = equality();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparision();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparision();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparision() {
        Expr expr = shift();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = shift();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr shift() {
        Expr expr = term();

        while (match(TokenType.RIGHT_SHIFT, TokenType.LEFT_SHIFT)) {
            Token operator = previous();
            Expr right = term();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR, TokenType.MOD, TokenType.INTEGER_DIV)) {
            Token operator = previous();
            Expr right = unary();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS, TokenType.BITWISE_NOT)) {
            Token operator = previous();
            Expr right = unary();

            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(TokenType.FALSE))
            return new Expr.Literal(false);
        if (match(TokenType.TRUE))
            return new Expr.Literal(true);
        if (match(TokenType.NIL))
            return new Expr.Literal(null);

        if (match(TokenType.INT, TokenType.STRING, TokenType.DOUBLE)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.INDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect an Expressrion.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Ameya.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMI_COL) {
                return;
            }
            switch (peek().type) {
                case CLASS:
                case FUNC:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.INDENTIFIER, "Expect variable Name.");
        Expr initializer = null;

        if (match(TokenType.EQUAL)) {
            initializer = expression();
        } else if (checkAny(TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL,
                TokenType.STAR_EQUAL, TokenType.SLASH_EQUAL, TokenType.MOD_EQUAL,
                TokenType.LEFT_SHIFT_EQUAL, TokenType.RIGHT_SHIFT_EQUAL,
                TokenType.AND_EQUAL, TokenType.XOR_EQUAL, TokenType.OR_EQUAL, TokenType.INTEGER_DIV_EQUAL)) {

            Token badOperator = peek();
            throw error(badOperator, "Cannot use '" + badOperator.lexeme + "' to declare '" + name.lexeme
                    + "' — it isn't defined yet. Declare it first with 'var " + name.lexeme + " = <value>;', then use '"
                    + name.lexeme + " " + badOperator.lexeme + " ...;' as a separate statement.");
        }

        consume(TokenType.SEMI_COL, "Expect ';' after end of variable declaration.");
        return new Stmt.Variable(name, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.FOR)) {
            return forStmt();
        }
        if (match(TokenType.IF)) {
            return ifStmt();
        }
        if (match(TokenType.PRINT)) {
            return printStmt();
        }
        if (match(TokenType.WHILE)) {
            return whileStmt();
        }
        if (match(TokenType.CURLY_LEFT)) {
            return new Stmt.Block(block());
        }
        return expressionStmt();
    }

    private Stmt forStmt() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after For.");
        Stmt initializer;
        if (match(TokenType.SEMI_COL)) {
            initializer = null;
        }else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        }else {
            initializer = expressionStmt();
        }

        Expr condition = null;
        if (!check(TokenType.SEMI_COL)) {
            condition = expression();
        }
        consume(TokenType.SEMI_COL, "Except a ';' after consition.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect')' this after for clause.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                body,
                new Stmt.Expression(increment)
            ));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    initializer,
                    body
                )
            );
        }
        
        return body;
    }

    private Stmt ifStmt() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after If or Else if.");
        Expr conditionExpr = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' for closing condition block.");

        // consume(TokenType.CURLY_LEFT, "Expect '{' for as Block Initilizer.");
        Stmt thenBranch = statement();
        // consume(TokenType.CURLY_RIGHT, "Expext '}' as Block closer.");

        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(conditionExpr, thenBranch, elseBranch);
    }

    private Stmt printStmt() {
        consume(TokenType.LEFT_PAREN, "Expect a Left Parenthesis.");
        Expr value = expression();
        consume(TokenType.RIGHT_PAREN, "Expect a Closing Parenthesis')''.");
        consume(TokenType.SEMI_COL, "Expected a Semi-Column after Print Statement.");

        return new Stmt.Print(value);
    }

    private Stmt whileStmt() {
        consume(TokenType.LEFT_PAREN, "Expect '(' Left Parenthesis.");
        Expr condiExpr = expression();
        consume(TokenType.RIGHT_PAREN, "Expect Closing Parenthesis ')'.");

        // consume(TokenType.CURLY_LEFT, "Expect while block initializer'{'.");
        Stmt whileBlock = statement();
        // consume(TokenType.CURLY_RIGHT, "Expect block ending brace '}'.");

        return new Stmt.While(condiExpr, whileBlock);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.CURLY_RIGHT) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.CURLY_RIGHT, "Expect '}' after Block.");
        return statements;
    }

    private Stmt expressionStmt() {
        Expr expr = expression();
        consume(TokenType.SEMI_COL, "Expected a Semi-Column after Expression.");

        return new Stmt.Expression(expr);
    }

    private Token compoundOperatorToken(Token operatorToken) {
        TokenType binaryType;
        String binaryLexeme;

        switch (operatorToken.type) {
            case PLUS_EQUAL:
                binaryType = TokenType.PLUS;
                binaryLexeme = "+";
                break;
            case MINUS_EQUAL:
                binaryType = TokenType.MINUS;
                binaryLexeme = "-";
                break;
            case STAR_EQUAL:
                binaryType = TokenType.STAR;
                binaryLexeme = "*";
                break;
            case SLASH_EQUAL:
                binaryType = TokenType.SLASH;
                binaryLexeme = "/";
                break;
            case MOD_EQUAL:
                binaryType = TokenType.MOD;
                binaryLexeme = "%";
                break;
            case LEFT_SHIFT_EQUAL:
                binaryType = TokenType.LEFT_SHIFT;
                binaryLexeme = "<<";
                break;
            case RIGHT_SHIFT_EQUAL:
                binaryType = TokenType.RIGHT_SHIFT;
                binaryLexeme = ">>";
                break;
            case AND_EQUAL:
                binaryType = TokenType.BITWISE_AND;
                binaryLexeme = "&";
                break;
            case OR_EQUAL:
                binaryType = TokenType.BITWISE_OR;
                binaryLexeme = "|";
                break;
            case XOR_EQUAL:
                binaryType = TokenType.BITWISE_XOR;
                binaryLexeme = "^";
                break;
            case INTEGER_DIV_EQUAL :
                binaryType = TokenType.INTEGER_DIV;
                binaryLexeme = "//";
                break;
            default:
                throw error(operatorToken, "Unsupported compound assignment operator.");
        }

        return new Token(binaryType, binaryLexeme, null, operatorToken.line);
    }
}