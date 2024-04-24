package de.tu_darmstadt.stg.mudetect.aug.model.data;

import org.eclipse.jdt.core.dom.ASTNode;

import de.tu_darmstadt.stg.mudetect.aug.model.BaseNode;
import de.tu_darmstadt.stg.mudetect.aug.model.DataNode;
import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class LiteralNode extends BaseNode implements DataNode {
    private final String dataType;
    private final String dataValue;

    public LiteralNode(String dataType, String dataValue,  ASTNode astNode) {
        this.dataType = dataType;
        
        
        if (dataType.equals("String")) {
        	this.dataValue = dataValue.replaceAll("\n", " "); // HJ: no newlines!
        } else {
        	this.dataValue = dataValue;
        }
        
        this.astNode = astNode;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getValue() {
        return dataValue;
    }

    @Override
    public String getType() {
        return dataType;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
