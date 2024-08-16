
package com.craftinginterpreters.lox;

import java.util.List;

public abstract class Stmt {

    interface Visitor<T> {
        public Void visitExpressionStmt(Expression expressionStatement);
        public Void visitPrintStmt(Print printStatement);
        public Void visitVarStmt(Var var);
        public Void visitBlockStmt(Block block);
        public Void visitIfStmt(If ifStmt);
        public Void visitWhileStatement(While whileStmt);
        public Void visitFunctionStmt(Function function);
        public Void visitReturnStmt(Return returnStmt);
    }

    abstract <T> void accept(Visitor<T> visitor);

    public static class Expression extends Stmt {
        final Expr expression;

        Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        <T> void accept(Visitor<T> visitor) {
            visitor.visitExpressionStmt(this);
        }
    }

    public static class Print extends Stmt {
        final Expr expression;

        Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        <T> void accept(Visitor<T> visitor) {
            visitor.visitPrintStmt(this);
        }
    }

    public static class Var extends Stmt {
        final Token name;
        final Expr initializer;

        Var(Token name,Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        <T> void accept(Visitor<T> visitor) {
            visitor.visitVarStmt(this);
        }
    }

    public static class Block extends Stmt {
        List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        <T> void accept(Visitor<T> visitor) {
            visitor.visitBlockStmt(this);
        }
    }

    public static class If extends Stmt {
        final Expr condition;
        final Stmt thenStatements;
        final Stmt elseStatements;

        If(Expr condition, Stmt thenStatements, Stmt elseStatements) {
            this.condition = condition;
            this.thenStatements = thenStatements;
            this.elseStatements = elseStatements;
        }

        @Override
        <T> void accept(Visitor<T> visitor) {
            visitor.visitIfStmt(this);
        }
    }

    public static class While extends Stmt {
        final Expr condition;
        final Stmt whileBlock;

        While(Expr condition, Stmt whileBlock) {
            this.condition = condition;
            this.whileBlock = whileBlock;
        }

        @Override
        <T> void accept(Visitor<T> visitor) {
            visitor.visitWhileStatement(this);
        }
    }

    public static class Function extends Stmt {
        final Token fnName;
        final List<Token> parameters;
        final List<Stmt> body;

        Function(Token fnName, List<Token> parameters, List<Stmt> body) {
            this.fnName = fnName;
            this.parameters = parameters;
            this.body = body;
        }

        @Override
        <T> void accept(Visitor<T> visitor) {
            visitor.visitFunctionStmt(this);
        }
    }

    public static class Return extends Stmt {
        final Token returnKeyword;
        final Expr returnExpression;

        Return(Token returnKeyword, Expr returnExpression) {
            this.returnKeyword = returnKeyword;
            this.returnExpression = returnExpression;
        }

        @Override
        <T> void accept(Visitor<T> visitor) {
            visitor.visitReturnStmt(this);
        }
    }

}
