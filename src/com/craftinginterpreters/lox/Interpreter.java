package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object> {
    void interpret(Expr expression) {
        try {
            Object output = evaluate(expression);
            System.out.println(stringify(output));
        } catch (RuntimeError e) {
            Lox.runtimeError(e);
        }
    }

    private String stringify(Object value) {
        if (value == null) return "nil";

        if (value instanceof Double) {
            String text = value.toString();
            return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        }
        return value.toString();
    }

    private Object evaluate(Expr expression) {
        return expression.accept(this);
    }

    @Override
    public Object visitUnary(Expr.Unary expr) {
        Object eval = evaluate(expr.right);
        return switch (expr.operator.type) {
            case BANG -> !isTruthy(eval);
            case MINUS -> {
                checkNumberOperands(expr.operator, eval);
                yield -(double) eval;
            }
            default -> null;
        };
    }

    @Override
    public Object visitBinary(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        return switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left - (double) right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left * (double) right;
            }
            case SLASH -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left / (double) right;
            }
            case PLUS -> {
                if (left instanceof String && right instanceof String) {
                    yield (String)left + (String) right;
                }
                if (left instanceof Double && right instanceof Double) {
                    yield (double) left + (double) right;
                }
                throw new RuntimeError(
                        expr.operator,
                        "Both operands should either be Strings or Numbers"
                );
            }
            case LESS -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left <= (double) right;
            }
            case GREATER -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left >= (double) right;
            }
            case EQUAL_EQUAL -> isEqual(left, right);
            case BANG_EQUAL -> !isEqual(left, right);
            case COMMA -> right;
            default -> null;
        };
    }

    @Override
    public Object visitLiteral(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGrouping(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitTernary(Expr.Ternary expr) {
        Object condition = evaluate(expr.left);
        return isTruthy((condition)) ? evaluate(expr.mid) : evaluate(expr.right);
    }

    /**
     * Everything except null and false is true.
     */
    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (boolean) value;
        return true;
    }

    private boolean isEqual(Object first, Object second) {
        if (first == null && second == null) return true;
        if (first == null || second == null) return false;

        return first.equals(second);
    }

    private void checkNumberOperands(Token token, Object value) {
        if (value instanceof Double) return;
        throw new RuntimeError(token, "Operand must be a number.");
    }

    private void checkNumberOperands(Token token, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(token, "Operands must be numbers.");
    }
}
