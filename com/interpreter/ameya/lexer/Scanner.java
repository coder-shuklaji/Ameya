package com.interpreter.ameya.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.interpreter.ameya.Ameya;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();

        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);
        // keywords.put("not", TokenType.NOT);
        keywords.put("xor", TokenType.XOR);

        keywords.put("for", TokenType.FOR);
        keywords.put("while", TokenType.WHILE);
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("elif", TokenType.ELIF);

        keywords.put("continue", TokenType.CONTINUE);
        keywords.put("break", TokenType.BREAK);
        keywords.put("print", TokenType.PRINT);

        keywords.put("func", TokenType.FUNC);
        keywords.put("return", TokenType.RETURN);

        keywords.put("class", TokenType.CLASS);
        keywords.put("this", TokenType.THIS);
        keywords.put("super", TokenType.SUPER);

        keywords.put("var", TokenType.VAR);
        keywords.put("const", TokenType.CONST);

        keywords.put("nil", TokenType.NIL);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= this.source.length();
    }

    private void scanToken() {
        char ch = advance();
        switch (ch) {
            case '\n':
                line++;
                break;
            case '\r':
                break;
            case '\t':
                break;
            case ' ':
                break;
            case '#':
                if (match('#')) {
                    while (!(peek() == '#' && peekNext() == '#') && !isAtEnd()) {
                        if (peek() == '\n')
                            line++;
                        advance();
                    }

                    if (!isAtEnd()) {
                        advance();
                        advance();
                    }
                } else {
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                }
                break;
            case '(':
                addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                break;
            case '{':
                addToken(TokenType.CURLY_LEFT);
                break;
            case '}':
                addToken(TokenType.CURLY_RIGHT);
                break;
            case ',':
                addToken(TokenType.COMMA);
                break;
            case '.':
                addToken(TokenType.DOT);
                break;
            case '~':
                addToken(TokenType.BITWISE_NOT);
                break;
            case '+':
                addToken(match('=') ? TokenType.PLUS_EQUAL : TokenType.PLUS);
                break;
            case '-':
                addToken(match('=') ? TokenType.MINUS_EQUAL : TokenType.MINUS);
                break;
            case '*':
                addToken(match('=') ? TokenType.STAR_EQUAL : TokenType.STAR);
                break;
            case '%':
                addToken(match('=') ? TokenType.MOD_EQUAL : TokenType.MOD);
                break;
            case '/':
                if(match('/')) {
                    addToken(match('=') ? TokenType.INTEGER_DIV_EQUAL : TokenType.INTEGER_DIV);
                }else {
                    addToken(match('=') ? TokenType.SLASH_EQUAL : TokenType.SLASH);
                }
                break;
            case ';':
                addToken(TokenType.SEMI_COL);
                break;
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '>':
                scanGreaterOrRight();
                break;
            case '<':
                scanLessOrLeft();
                break;
            case '&':
                addToken(match('=') ? TokenType.AND_EQUAL : TokenType.BITWISE_AND);
                break;
            case '|':
                addToken(match('=') ? TokenType.OR_EQUAL : TokenType.BITWISE_OR);
                break;
            case '^':
                addToken(match('=') ? TokenType.XOR_EQUAL : TokenType.BITWISE_XOR);
                break;
            case '"':
                string();
                break;
            default:
                if (isDigit(ch)) {
                    number();
                } else if (isAlphaNumeric(ch)) {
                    indentifier();
                } else {
                    Ameya.error(line, "Unexpected Error!");
                }
                break;
        }
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private char peek() {
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    private boolean match(char expected) {
        if (isAtEnd())
            return false;
        if (source.charAt(current) != expected)
            return false;

        current++;
        return true;
    }

    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    private void scanGreaterOrRight() {
        if (match('>')) {
            addToken(match('=') ? TokenType.RIGHT_SHIFT_EQUAL : TokenType.RIGHT_SHIFT);
        } else {
            addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
        }
    }

    private void scanLessOrLeft() {
        if (match('<')) {
            addToken(match('=') ? TokenType.LEFT_SHIFT_EQUAL : TokenType.LEFT_SHIFT);
        } else {
            addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n')
                line++;
            advance();
        }

        if (isAtEnd()) {
            Ameya.error(line, "Unterminated String.");
            return;
        }

        advance();

        String rawString = source.substring(start + 1, current - 1);
        String value = refinedString(rawString);
        addToken(TokenType.STRING, value);
    }

    private String refinedString(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(++i);
                switch (next) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case '"':
                        sb.append('\"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    default:
                        sb.append(next);
                        break;
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private boolean isAlpha(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch == '_');
    }

    private boolean isAlphaNumeric(char ch) {
        return isAlpha(ch) || isDigit(ch);
    }

    private void number() {
        while (isDigit(peek()))
            advance();

        boolean isDouble = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isDouble = true;
            advance();
            while (isDigit(peek()))
                advance();
        }
        if (isDouble) {
            addToken(TokenType.DOUBLE, Double.parseDouble(source.substring(start, current)));
        } else {
            addToken(TokenType.INT, Long.parseLong(source.substring(start, current)));
        }
    }

    private void indentifier() {
        while (isAlphaNumeric(peek()))
            advance();

        String key = source.substring(start, current);
        TokenType type = keywords.get(key);
        if (type == null)
            type = TokenType.INDENTIFIER;

        addToken(type);
    }
}