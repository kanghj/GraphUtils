package de.tu_darmstadt.stg.mudetect.model;

import de.tu_darmstadt.stg.mudetect.Instance;
import egroum.EGroumDataEdge;
import egroum.EGroumEdge;
import egroum.EGroumNode;

import java.util.HashMap;
import java.util.Map;

public class TestInstanceBuilder {

    public static TestInstanceBuilder buildInstance(TestAUGBuilder targetAUGBuilder, TestAUGBuilder patternAUGBuilder) {
        return new TestInstanceBuilder(targetAUGBuilder, patternAUGBuilder);
    }

    private final TestAUGBuilder targetAUGBuilder;
    private final TestAUGBuilder patternAUGBuilder;
    private final Map<EGroumNode, EGroumNode> targetNodeByPatternNode = new HashMap<>();
    private final Map<EGroumEdge, EGroumEdge> targetEdgeByPatternEdge = new HashMap<>();

    private TestInstanceBuilder(TestAUGBuilder targetAUGBuilder, TestAUGBuilder patternAUGBuilder) {
        this.targetAUGBuilder = targetAUGBuilder;
        this.patternAUGBuilder = patternAUGBuilder;
    }

    public TestInstanceBuilder withNode(String targetNodeId, String patternNodeId) {
        EGroumNode targetNode = targetAUGBuilder.getNode(targetNodeId);
        if (targetNodeByPatternNode.containsValue(targetNode)) {
            throw new IllegalArgumentException("Target node '" + targetNodeId + "' is already mapped.");
        }
        EGroumNode patternNode = patternAUGBuilder.getNode(patternNodeId);
        if (targetNodeByPatternNode.containsKey(patternNode)) {
            throw new IllegalArgumentException("Pattern node '" + patternNodeId + "' is already mapped.");
        }
        targetNodeByPatternNode.put(patternNode, targetNode);
        return this;
    }

    public TestInstanceBuilder withEdge(String targetSourceNodeId, String patternSourceNodeId, EGroumDataEdge.Type type, String targetTargetNodeId, String patternTargetNodeId) {
        targetEdgeByPatternEdge.put(
                patternAUGBuilder.getEdge(patternSourceNodeId, type, patternTargetNodeId),
                targetAUGBuilder.getEdge(targetSourceNodeId, type, targetTargetNodeId));
        return this;
    }

    public Instance build() {
        return new Instance(patternAUGBuilder.build(), targetAUGBuilder.build(), targetNodeByPatternNode, targetEdgeByPatternEdge);
    }
}