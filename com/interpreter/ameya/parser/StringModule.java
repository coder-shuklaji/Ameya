package com.interpreter.ameya.parser;

import com.interpreter.ameya.lexer.Token;

public class StringModule {
    public static String repeateString(String str, long times, Token operator) {
        if (times < 0) {
            throw new RuntimeError(operator,
                    "Cannot repeat \"" + str + "\" " + times + "times: repeat count must be zero or positive.");
        }
        if (times == 0 || str.isEmpty()) {
            return "";
        }

        long capacity = (long) str.length() * times;
        if (capacity > Integer.MAX_VALUE) {
            throw new RuntimeError(operator,
                    "Cannot repeat a String of length " + str.length() + " " + times + " times: the result would be "
                            + capacity + " characters long, which exceeds the maximum of " + Integer.MAX_VALUE + ".");
        }

        StringBuilder sb = new StringBuilder((int) capacity);
        for (long i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static String concatenateString(String str1, String str2) {
        StringBuilder sb = new StringBuilder(str1.length() + str2.length());
        sb.append(str1);
        sb.append(str2);
        return sb.toString();
    }
}
