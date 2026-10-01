package com.interpreter.ameya.parser;

import com.interpreter.ameya.lexer.Token;

public class RuntimeError extends RuntimeException{
    public final Token token;

    RuntimeError (Token token, String message) {
        super(message);
        this.token = token;
    }
}
