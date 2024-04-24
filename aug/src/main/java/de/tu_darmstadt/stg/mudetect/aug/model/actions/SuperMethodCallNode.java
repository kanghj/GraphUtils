package de.tu_darmstadt.stg.mudetect.aug.model.actions;

import org.eclipse.jdt.core.dom.ASTNode;

import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class SuperMethodCallNode extends MethodCallNode {
    public SuperMethodCallNode(String declaringTypeName, String methodSignature) {
        super(declaringTypeName, methodSignature);
    }

    public SuperMethodCallNode(String declaringTypeName, String methodSignature, int sourceLineNumber, ASTNode astNode) {
        super(declaringTypeName, methodSignature, sourceLineNumber, astNode);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
