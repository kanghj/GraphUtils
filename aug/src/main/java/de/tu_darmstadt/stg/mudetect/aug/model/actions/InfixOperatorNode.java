package de.tu_darmstadt.stg.mudetect.aug.model.actions;

import org.eclipse.jdt.core.dom.ASTNode;

import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class InfixOperatorNode extends OperatorNode {
    public InfixOperatorNode(String operator) {
        super(operator);
    }

    public InfixOperatorNode(String operator, int sourceLineNumber, ASTNode astNode) {
        super(operator, sourceLineNumber, astNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
