package de.tu_darmstadt.stg.mudetect.aug.model.data;

import org.eclipse.jdt.core.dom.ASTNode;

import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class ExceptionNode extends VariableNode {
    public ExceptionNode(String exceptionType, String variableName, ASTNode astNode) {
        super(exceptionType, variableName, astNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
