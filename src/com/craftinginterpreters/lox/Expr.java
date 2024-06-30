
package com.craftinginterpreters.lox;

public abstract class Expr {

    interface Visitor<T> {
        T visitUnary(Unary expr);
        T visitBinary(Binary expr);
        T visitLiteral(Literal expr);
        T visitGrouping(Grouping expr);
    }

    public static class Unary extends Expr {
        final Token operator;
        final Expr right;

        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitUnary(this);
        }
    }

    public static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;

        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitBinary(this);
        }
    }

    public static class Literal extends Expr {
        final Object value;

        Literal(Object value) {
            this.value = value;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitLiteral(this);
        }
    }

    public static class Grouping extends Expr {
        final Expr expression;

        Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitGrouping(this);
        }
    }

    abstract <T> T accept(Visitor<T> visitor);
}
