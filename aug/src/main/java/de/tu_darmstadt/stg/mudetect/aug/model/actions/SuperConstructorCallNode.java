package de.tu_darmstadt.stg.mudetect.aug.model.actions;

import org.eclipse.jdt.core.dom.ASTNode;

import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class SuperConstructorCallNode extends ConstructorCallNode {
    public SuperConstructorCallNode(String superTypeName) {
        super(superTypeName);
    }

    public SuperConstructorCallNode(String superTypeName, int sourceLineNumber, ASTNode astNode) {
        super(superTypeName, sourceLineNumber, astNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
