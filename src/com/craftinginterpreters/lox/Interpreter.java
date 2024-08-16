package com.craftinginterpreters.lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            executeStatements(statements);
        } catch (RuntimeError e) {
            Lox.runtimeError(e);
        }
    }

    private void executeStatements(List<Stmt> statements) {
        for (Stmt statement : statements) {
            executeStatement(statement);
        }
    }

    private void executeStatement(Stmt statement) {
        statement.accept(this);
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
                double denominator = (double) right;
                if (denominator == 0) {
                    throw new RuntimeError(expr.operator, "Cannot divide by zero.");
                }
                yield (double) left / (double) right;
            }
            case PLUS -> {
                if (left instanceof String || right instanceof String) {
                    yield stringify(left) + stringify(right);
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

    @Override
    public Object visitVariable(Expr.Variable variable) {
        return environment.get(variable.identifier);
    }

    @Override
    public Object visitAssignment(Expr.Assign assign) {
        Object rValue = evaluate(assign.value);
        environment.assign(assign.var, rValue);
        return rValue;
    }

    @Override
    public Object visitLogicalOperator(Expr.Logical logical) {
        Object left = evaluate(logical.left);
        if (logical.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(logical.right);
    }

    @Override
    public Object visitCallExpr(Expr.Call call) {
        Object callee = evaluate(call.callee);
        List<Object> args = call.args.stream().map(this::evaluate).toList();
        if (!(callee instanceof LoxCallable function)) {
            throw new RuntimeError(call.paren, "Can only call functions and classes");
        }
        if (args.size() != function.arity()) {
            throw new RuntimeError(call.paren,
                    "Expected " + function.arity() + " arguments but received " + args.size());
        }
        return function.call(this, args);
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

    @Override
    public Void visitExpressionStmt(Stmt.Expression expressionStatement) {
        evaluate(expressionStatement.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print printStatement) {
        System.out.println(stringify(evaluate(printStatement.expression)));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var var) {
        environment.define(var.name.lexeme, var.initializer != null ? evaluate(var.initializer) : null);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block block) {
        executeBlock(block.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If ifStmt) {
        Object conditionValue = evaluate(ifStmt.condition);
        if (isTruthy(conditionValue)) {
            executeStatement(ifStmt.thenStatements);
        } else {
            if (ifStmt.elseStatements != null) executeStatement(ifStmt.elseStatements);
        }
        return null;
    }

    @Override
    public Void visitWhileStatement(Stmt.While whileStmt) {
        while (isTruthy(evaluate(whileStmt.condition))) {
            executeStatement(whileStmt.whileBlock);
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function function) {
        LoxFunction fun = new LoxFunction(function, environment);
        environment.define(function.fnName.lexeme, fun);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return returnStmt) {
        Object value = returnStmt.returnExpression == null ? null : evaluate(returnStmt.returnExpression);
        throw new Return(value);
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            executeStatements(statements);
        } finally {
            this.environment = previous;
        }
    }
}
