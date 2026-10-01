package com.interpreter.ameya.parser;

import java.util.List;

import com.interpreter.ameya.Ameya;
import com.interpreter.ameya.lexer.Token;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    public void interpret(Expr expression) {
        try {
            Object obj = evaluate(expression);
            System.out.print(stringify(obj));
        } catch (RuntimeError error) {
            Ameya.runtimeError(error);
        }
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Ameya.runtimeError(error);
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (toBool(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(value);
        return null;
    }

    @Override
    public Void visitVariableStmt(Stmt.Variable stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (toBool(evaluate(stmt.condition))) {
            execute(stmt.whileBody);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS -> {
                if (right instanceof Double)
                    return -(Double) right;
                if (right instanceof Long)
                    return -(Long) right;
                throw new RuntimeError(expr.operator,
                        "Unary '-' requires a number, but got " + describe(right) + ".");
            }
            case BANG -> {
                return !toBool(right);
            }
            case BITWISE_NOT -> {
                if (right instanceof Long) {
                    return ~(Long) right;
                }
                throw new RuntimeError(expr.operator,
                        "Unary '~' requires an Integer, but got " + describe(right) + ".");
            }
            default -> {
                throw new RuntimeError(expr.operator,
                        "'" + expr.operator.lexeme + "' is not a valid unary operator (expected one of: -, !, ~).");
            }
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        return switch (expr.operator.type) {
            case STAR -> multiply(left, right, expr.operator);
            case MOD -> mod(left, right, expr.operator);
            case MINUS -> subtract(left, right, expr.operator);
            case SLASH -> divide(left, right, expr.operator);
            case INTEGER_DIV -> integerDivide(left, right, expr.operator);
            case PLUS -> add(left, right, expr.operator);
            case EQUAL_EQUAL -> equalEqual(left, right);
            case BANG_EQUAL -> bangEqual(left, right);
            case GREATER -> greater(left, right, expr.operator);
            case LESS -> less(left, right, expr.operator);
            case GREATER_EQUAL -> greaterEqual(left, right, expr.operator);
            case LESS_EQUAL -> lessEqual(left, right, expr.operator);
            case LEFT_SHIFT -> leftShift(left, right, expr.operator);
            case RIGHT_SHIFT -> rightShift(left, right, expr.operator);
            case BITWISE_AND -> bitwiseAnd(left, right, expr.operator);
            case BITWISE_OR -> bitwiseOr(left, right, expr.operator);
            case BITWISE_XOR -> bitwiseXor(left, right, expr.operator);
            // case AND -> logicalAnd(left, right, expr.operator);
            // case OR -> logicalOr(left, right, expr.operator);
            // case XOR -> logicalXor(left, right, expr.operator);
            default -> throw new RuntimeError(expr.operator,
                    "'" + expr.operator.lexeme + "' is not a supported binary operator.");
        };
    }

    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        switch (expr.operator.type) {
            case AND -> {
                if (!toBool(left))
                    return left;
                return evaluate(expr.right);
            }
            case OR -> {
                if (toBool(left))
                    return left;
                return evaluate(expr.right);
            }
            case XOR -> {
                Object right = evaluate(expr.right);
                return toBool(left) ^ toBool(right);
            }
            default -> throw new RuntimeError(expr.operator, "Expected one of the logical operators "
                    + "'and', 'or', or 'xor' between expressions, " + "but found '" + expr.operator.lexeme + "'.");
        }
    }

    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment prevEnvironment = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = prevEnvironment;
        }
    }

    // private Boolean isTruthy(Object object) {
    // if (object == null)
    // return false;
    // if (object instanceof Boolean)
    // return (Boolean) object;
    // return true;
    // }

    private String stringify(Object obj) {
        if (obj == null)
            return "nil";
        if (obj instanceof Double) {
            String txt = obj.toString();
            if (txt.endsWith(".0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            return txt;
        }
        return obj.toString();
    }

    private String typeName(Object obj) {
        if (obj == null)
            return "nil";
        if (obj instanceof Long)
            return "Integer";
        if (obj instanceof Double)
            return "Decimal";
        if (obj instanceof String)
            return "String";
        if (obj instanceof Boolean)
            return "Boolean";
        return obj.getClass().getSimpleName();
    }

    private String describe(Object obj) {
        if (obj == null)
            return "nil";
        if (obj instanceof String)
            return "String (\"" + obj + "\")";
        return typeName(obj) + " (" + stringify(obj) + ")";
    }

    private Object multiply(Object left, Object right, Token operator) {
        if (left instanceof String && right instanceof Long)
            return StringModule.repeateString((String) left, (Long) right, operator);
        if (left instanceof String && right instanceof Double)
            return StringModule.repeateString((String) left, ((Double) right).intValue(), operator);
        if (right instanceof String && left instanceof Long)
            return StringModule.repeateString((String) right, (Long) left, operator);
        if (right instanceof String && left instanceof Double)
            return StringModule.repeateString((String) right, ((Double) left).intValue(), operator);
        if (left instanceof Long && right instanceof Long)
            return (Long) left * (Long) right;
        if (left instanceof Number && right instanceof Number)
            return toDouble(left) * toDouble(right);
        throw new RuntimeError(operator,
                "Cannot apply '*' to " + describe(left) + " and " + describe(right)
                        + ". Expected two numbers, or a String with an Integer for repetition.");
    }

    private Object mod(Object left, Object right, Token operator) {
        if (left instanceof Long && right instanceof Long) {
            return (Long) toInt(left, operator) % toInt(right, operator);
        }
        if (left instanceof Number && right instanceof Number) {
            return (Double) toDouble(left) % toDouble(right);
        }
        throw new RuntimeError(operator,
                "Cannot apply '%' to " + describe(left) + " and " + describe(right)
                        + ". Both operands must be numbers.");
    }

    private Object subtract(Object left, Object right, Token operator) {
        if (left instanceof Long && right instanceof Long)
            return (Long) left - (Long) right;
        if (left instanceof Number && right instanceof Number)
            return toDouble(left) - toDouble(right);
        throw new RuntimeError(operator,
                "Cannot apply '-' to " + describe(left) + " and " + describe(right)
                        + ". Both operands must be numbers.");
    }

    private Object divide(Object left, Object right, Token operator) {
        if (left instanceof Number && right instanceof Number) {
            double divisor = toDouble(right, operator);
            if (divisor != 0)
                return toDouble(left, operator) / divisor;
            throw new RuntimeError(operator,
                    "Division by zero: cannot divide " + describe(left) + " by " + describe(right) + ".");
        }
        throw new RuntimeError(operator,
                "Cannot apply '/' to " + describe(left) + " and " + describe(right)
                        + ". Both operands must be numbers.");
    }

    private Object integerDivide(Object left, Object right, Token operator) {
        if (left instanceof Number && right instanceof Number) {
            long dividend = (long) toDouble(left, operator);
            long divisor = (long) toDouble(right, operator);

            if (divisor == 0) {
                throw new RuntimeError(operator,
                        "Division by zero: cannot divide " + describe(left) + " by " + describe(right) + ".");
            }
            return dividend / divisor;
        }

        throw new RuntimeError(operator,
                "Cannot apply '//' to " + describe(left) + " and " + describe(right)
                        + ". Both operands must be numbers.");
    }

    private Object add(Object left, Object right, Token operator) {
        if (left instanceof String || right instanceof String) {
            return stringify(left) + stringify(right);
        }

        if (left instanceof Long && right instanceof Long) {
            return (Long) left + (Long) right;
        }

        if (left instanceof Number && right instanceof Number) {
            return toDouble(left, operator) + toDouble(right, operator);
        }

        throw new RuntimeError(operator,
                "Cannot apply '+' to " + describe(left) + " and " + describe(right)
                        + ". Expected both operands to be numbers, or at least one String.");
    }

    private Object equalEqual(Object left, Object right) {
        if (left == null && right == null)
            return true;
        if (left == null || right == null)
            return false;
        return left.equals(right);
    }

    private Object bangEqual(Object left, Object right) {
        if (left == null && right == null)
            return false;
        if (left == null || right == null)
            return true;
        return !left.equals(right);
    }

    private Object greater(Object left, Object right, Token operator) {
        return toDouble(left, operator) > toDouble(right, operator);
    }

    private Object less(Object left, Object right, Token operator) {
        return toDouble(left, operator) < toDouble(right, operator);
    }

    private Object greaterEqual(Object left, Object right, Token operator) {
        return toDouble(left, operator) >= toDouble(right, operator);
    }

    private Object lessEqual(Object left, Object right, Token operator) {
        return toDouble(left, operator) <= toDouble(right, operator);
    }

    private Object leftShift(Object left, Object right, Token operator) {
        return toInt(left, operator) << toInt(right, operator);
    }

    private Object rightShift(Object left, Object right, Token operator) {
        return toInt(left, operator) >> toInt(right, operator);
    }

    private Object bitwiseAnd(Object left, Object right, Token operator) {
        if (left instanceof Long || right instanceof Long) {
            return (Long) toInt(left, operator) & toInt(right, operator);
        }
        throw new RuntimeError(operator,
                "Cannot apply '" + operator.lexeme + "' to " + describe(left) + " and " + describe(right)
                        + ". Bitwise operators require Integer operands.");
    }

    private Object bitwiseOr(Object left, Object right, Token operator) {
        if (left instanceof Long || right instanceof Long) {
            return (Long) toInt(left, operator) | toInt(right, operator);
        }
        throw new RuntimeError(operator,
                "Cannot apply '" + operator.lexeme + "' to " + describe(left) + " and " + describe(right)
                        + ". Bitwise operators require Integer operands.");
    }

    private Object bitwiseXor(Object left, Object right, Token operator) {
        if (left instanceof Long || right instanceof Long) {
            return (Long) toInt(left, operator) ^ toInt(right, operator);
        }
        throw new RuntimeError(operator,
                "Cannot apply '" + operator.lexeme + "' to " + describe(left) + " and " + describe(right)
                        + ". Bitwise operators require Integer operands.");
    }

    private double toDouble(Object obj) {
        if (obj instanceof Long)
            return ((Long) obj).doubleValue();
        if (obj instanceof Double)
            return (Double) obj;
        throw new RuntimeError(null,
                "'*' expected a number, but got " + describe(obj) + ".");
    }

    private long toInt(Object obj, Token operator) {
        if (obj instanceof Long)
            return (Long) obj;
        if (obj instanceof Double)
            return ((Double) obj).intValue();
        throw new RuntimeError(operator,
                "'" + operator.lexeme + "' expected a number, but got " + describe(obj) + ".");
    }

    private double toDouble(Object obj, Token operator) {
        if (obj instanceof Long)
            return ((Long) obj).doubleValue();
        if (obj instanceof Double)
            return (Double) obj;
        throw new RuntimeError(operator,
                "'" + operator.lexeme + "' expected a number, but got " + describe(obj) + ".");
    }

    private boolean toBool(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof Number) {
            double val = ((Number) obj).doubleValue();
            return val != 0 && !Double.isNaN(val);
        }
        if (obj instanceof String) {
            return !((String) obj).isEmpty();
        }
        return true;
    }
}