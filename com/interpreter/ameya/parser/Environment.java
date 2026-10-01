package com.interpreter.ameya.parser;

import java.util.Map;
import java.util.HashMap;

import com.interpreter.ameya.lexer.Token;

public class Environment {
    final Environment enclosed;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this.enclosed = null;
    }

    Environment(Environment enclosed){
        this.enclosed = enclosed;
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosed != null) return enclosed.get(name);

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'. Declare it first with 'var " + name.lexeme
                        + " = <value>;' before using it.");
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosed != null) {
            enclosed.assign(name, value);
            return;
        }
        
        throw new RuntimeError(name,
                "Cannot assign to undefined variable '" + name.lexeme + "'. Declare it first with 'var " + name.lexeme
                        + " = <value>;'.");
    }
}
