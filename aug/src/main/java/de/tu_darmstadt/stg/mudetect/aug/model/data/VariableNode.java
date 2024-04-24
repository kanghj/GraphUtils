package de.tu_darmstadt.stg.mudetect.aug.model.data;

import org.eclipse.jdt.core.dom.ASTNode;

import de.tu_darmstadt.stg.mudetect.aug.model.BaseNode;
import de.tu_darmstadt.stg.mudetect.aug.model.DataNode;
import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class VariableNode extends BaseNode implements DataNode {
    private final String variableType;
    private final String variableName;

    public VariableNode(String variableType, String variableName, ASTNode astNode) {
        this.variableType = variableType;
        this.variableName = variableName;
        
        this.astNode = astNode;
    }

    @Override
    public String getName() {
        return variableName;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getType() {
        return variableType;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
