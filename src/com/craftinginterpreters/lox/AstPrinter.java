package com.craftinginterpreters.lox;

public class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitUnary(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitBinary(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitLiteral(Expr.Literal expr) {
        return expr == null ? "nil" : expr.value.toString();
    }

    @Override
    public String visitGrouping(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitTernary(Expr.Ternary expr) {
        return parenthesize("?:", expr.left, expr.mid, expr.right);
    }

    private String parenthesize(String opName, Expr... expressions) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(opName);
        for (Expr expr: expressions) {
            builder
                    .append(" ")
                    .append(print(expr));
        }
        builder.append(")");
        return builder.toString();
    }
}
