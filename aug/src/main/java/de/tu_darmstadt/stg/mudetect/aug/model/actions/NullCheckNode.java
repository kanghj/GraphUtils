package de.tu_darmstadt.stg.mudetect.aug.model.actions;

import org.eclipse.jdt.core.dom.ASTNode;

import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class NullCheckNode extends InfixOperatorNode {
    public NullCheckNode() {
        super("<nullcheck>");
    }

    public NullCheckNode(int sourceLineNumber, ASTNode astNode) {
        super("<nullcheck>", sourceLineNumber, astNode);
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
