package smu.hongjin;

import java.util.Optional;

import org.jgrapht.graph.DirectedMultigraph;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.Edge;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.controlflow.FinallyEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.data.ConstantNode;

/**
 * For drawing as a dot file
 * 
 * @author kanghongjin
 *
 */
public class EnhancedAUGAsGraph extends DirectedMultigraph<Node, Edge> {

	public EnhancedAUGAsGraph() {
		super(Edge.class);
	}

	public void build(EnhancedAUG eaug) {

		for (Node node : eaug.aug.vertexSet()) {
			this.addVertex(node);
		}

		for (Edge edge : eaug.aug.edgeSet()) {
			this.addEdge(edge.getSource(), edge.getTarget(), edge);
		}
		
		if (eaug.interfaces.isEmpty()) {
			return; // done
		}
	}

}
