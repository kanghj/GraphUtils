package de.tu_darmstadt.stg.mudetect.aug.model.actions;

import org.eclipse.jdt.core.dom.ASTNode;

import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class ArrayCreationNode extends ConstructorCallNode {
    public ArrayCreationNode(String baseType) {
        super(baseType);
    }

    public ArrayCreationNode(String baseType, int sourceLineNumber, ASTNode astNode) {
        super(baseType, sourceLineNumber, astNode);
    }

    @Override
    public boolean isCoreAction() {
        return false;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
