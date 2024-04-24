package hongjin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.gson.Gson;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageGraph;
import de.tu_darmstadt.stg.mudetect.aug.model.BaseEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.BaseNode;
import de.tu_darmstadt.stg.mudetect.aug.model.Edge;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.Edge.Type;
import de.tu_darmstadt.stg.mudetect.aug.model.data.ConstantNode;
import de.tu_darmstadt.stg.mudetect.aug.model.data.VariableNode;
import de.tu_darmstadt.stg.mudetect.aug.model.dataflow.ParameterEdge;
import de.tu_darmstadt.stg.mudetect.aug.visitors.BaseAUGLabelProvider;
import smu.hongjin.EnhancedAUG;

/**
 * The GSpan subgraph miner expects the graph in a specific format. This class contains the code that does that formatting.
 * @author kanghongjin
 *
 */
public class SubgraphMiningFormatter {
	

//	public static void convert(Collection<APIUsageExample> augs, int i, Map<String, Integer> vertexLabels, Map<String, Integer> edgeLabels, BufferedWriter writer) throws IOException {
//		// along the way,
//		// we collect the labels of vertices and edges
//
//		
//		for (APIUsageExample aug : augs) {
//			writer.write("t " + "# " + i + "\n");
//
//			Map<Node, Integer> vertexNumbers = new HashMap<>();
//			
//			int nodeNumber = 0;
//			for (Node vertex : aug.vertexSet()) {
//				 String nodeLabel = new BaseAUGLabelProvider().getLabel(vertex);
//				 
//				 if (!vertexLabels.containsKey(nodeLabel)) {
//					 vertexLabels.put(nodeLabel, vertexLabels.size());	 
//				 }
//				 int nodeLabelIndex = vertexLabels.get(nodeLabel);
//				 
//				 writer.write("v " + nodeNumber + " " + nodeLabelIndex+ "\n");
//				 
//				 
//				 vertexNumbers.put(vertex, nodeNumber);
//				 
//				 nodeNumber+=1;
//			}
//			
//			//
//			
//			for (Edge edge : aug.edgeSet()) {
//				String edgeLabel = new BaseAUGLabelProvider().getLabel(edge);
//				
//				if (!edgeLabels.containsKey(edgeLabel)) {
//					edgeLabels.put(edgeLabel, edgeLabels.size());	 
//				}
//				int edgeLabelIndex = edgeLabels.get(edgeLabel);
//				 
//				
//				int sourceNumber = vertexNumbers.get(edge.getSource());
//				int targetNumber = vertexNumbers.get(edge.getTarget());
//				
//				writer.write("e " + sourceNumber + " " + targetNumber + " " + edgeLabelIndex+ "\n");
//			}
//			i++;
//		}
//		
//	}
//	
	
	public static int convert(Collection<EnhancedAUG> eaugs, Class<?> type ,int i, 
			Map<String, Integer> vertexLabels, Map<String, Integer> edgeLabels, 
			String fileId,
			Map<String, String> labels, int quantity, String subIdentifier, 
			BufferedWriter writer, BufferedWriter idMappingWriter, Map<Integer, String> startAndEndJsons) throws IOException {
		return convert(eaugs, i, vertexLabels, edgeLabels, Integer.valueOf(fileId), labels, quantity, subIdentifier, writer, idMappingWriter, startAndEndJsons);
		
	}
	/**
	 * This mutates vertexLabels and edgeLabels
	 * @param eaugs
	 * @param type
	 * @param i
	 * @param vertexLabels
	 * @param edgeLabels
	 * @param label
	 * @param quantity
	 * @param writer
	 * @param startAndEndJsons 
	 * @throws IOException
	 */
	public static int convert(Collection<EnhancedAUG> eaugs, int i, 
			Map<String, Integer> vertexLabels, Map<String, Integer> edgeLabels, 
			int instanceId,
			Map<String, String> labels, int quantity, String subIdentifier, 
			BufferedWriter writer, BufferedWriter idMappingWriter, Map<Integer, String> startAndEndJsons) throws IOException {
		
		if (edgeLabels.isEmpty()) {
			initEdgeLabels(edgeLabels);
		}
		
		// along the way,
		// we collect the labels of vertices and edges
		
		for (EnhancedAUG eaug : eaugs) {
			APIUsageGraph aug = eaug.aug;
			
			if (aug.vertexSet().size() == 0) {
				continue;
			}
			
			// 
			String labelId = instanceId + " - " + eaug.aug.name;
			String label = labels.get(labelId);
			if (label == null) {
				
				String possibleClazz = labelId.split(" - ")[1];
				int afterFirstDot = possibleClazz.indexOf(".");
				String classAfterFirstDot = possibleClazz.substring(afterFirstDot + 1);
				String newIdToTry = labelId.split(" - ")[0] + " - " + classAfterFirstDot; 
				label = labels.get(newIdToTry);
				if (label == null) {
					System.out.println("omitted due to missing label of labelId=" + labelId + " and (as ad-hoc backup) " + newIdToTry);
					continue;
				}
			}
//			if (label == null) throw new RuntimeException("missing label!");
			
			
			writer.write("t " + "# " + i + " " + label + " " + quantity + "\n");
			if (idMappingWriter != null) {
				idMappingWriter.write(subIdentifier + instanceId + "," + i + "," + labelId + "\n");
			}
			

			
			
			// 
			Map<String, Integer> expressionStart = new HashMap<>();
			Map<String, List<Integer>> expressionStartAdditional = new HashMap<>();
			Map<String, List<Integer>> expressionEndAdditional = new HashMap<>();
			Map<String, Integer> expressionStartLine = new HashMap<>();
			Map<String, Integer> expressionEnd = new HashMap<>();
			
			int earliest = Integer.MAX_VALUE;
			ASTNode methodDeclaration = null;
			ASTNode typeDeclaration = null;
			
			for (Node node : eaug.aug.vertexSet()) {
				BaseNode basenode = (BaseNode) node;
				if (basenode.astNode != null) {
					if (earliest > basenode.astNode.getStartPosition()) {
						earliest = basenode.astNode.getStartPosition();
						methodDeclaration = basenode.astNode;
						typeDeclaration = basenode.astNode;
					}
				}
			}
			
			// find methodDeclaration
			while (methodDeclaration != null && !(methodDeclaration instanceof MethodDeclaration)) {
				methodDeclaration = methodDeclaration.getParent();
//				earliest = methodDeclaration.getStartPosition();
			}
			
			// find typeDeclaration
			while (typeDeclaration != null && !(typeDeclaration instanceof TypeDeclaration)) {
				typeDeclaration = typeDeclaration.getParent();
//				earliest = earliestNode.getStartPosition();
			}

			// earliest = earliestNode.getParent().getStartPosition();
							
			for (Node node : eaug.aug.vertexSet()) {
				BaseNode basenode = (BaseNode) node;
				String nodeLabel = new BaseAUGLabelProvider().getLabel(basenode);
				if (basenode.astNode != null) {
					int startposition = basenode.astNode.getStartPosition() - methodDeclaration.getStartPosition();
					expressionStart.put(nodeLabel, startposition);
					if (!expressionStartAdditional.containsKey(nodeLabel)) {
						expressionStartAdditional.put(nodeLabel, new ArrayList<>());
					}
					expressionStartAdditional.get(nodeLabel).add(startposition);
					
					

					// convert to line number
					String source = ((APIUsageExample) eaug.aug).sourceCode;
					int startLine = source.substring(0, basenode.astNode.getStartPosition()).split("\n").length;
					expressionStartLine.put(nodeLabel, startLine);

					int end = basenode.astNode.getStartPosition() + basenode.astNode.getLength();
					expressionEnd.put(nodeLabel, end - methodDeclaration.getStartPosition());
					
					if (!expressionEndAdditional.containsKey(nodeLabel)) {
						expressionEndAdditional.put(nodeLabel, new ArrayList<>());
					}
					expressionEndAdditional.get(nodeLabel).add(end - methodDeclaration.getStartPosition());

				}
			}
			
			Map<Node, Node> unknownParameters = new HashMap<>();
			Map<Node, Edge> oldEdgesToRemove = new HashMap<>();
			for (Edge edge: eaug.aug.edgeSet()) {
				BaseEdge baseEdge = ((BaseEdge) edge);
				String sourceLabel = new BaseAUGLabelProvider().getLabel(baseEdge.getSource());
				String edgeType = 	new BaseAUGLabelProvider().getLabel(baseEdge);
				if (sourceLabel.equals("UNKNOWN") && edgeType.equals("para")) {
					unknownParameters.put(baseEdge.getSource(), baseEdge.getTarget());
					oldEdgesToRemove.put(baseEdge.getSource(), baseEdge);
				}
			}
			
			Set<Edge> edgesToAdd = new HashSet<>();
			Set<Edge> edgesToRemove = new HashSet<>();
			for (Edge edge: eaug.aug.edgeSet()) {
				// linked to an unknown parameter
				BaseEdge baseEdge = ((BaseEdge) edge);
				String edgeType = new BaseAUGLabelProvider().getLabel(baseEdge);
				if (edgeType.equals("def") && unknownParameters.containsKey(baseEdge.getTarget())) {
					
					Node newTarget = unknownParameters.get(baseEdge.getTarget());
					BaseEdge newEdge = new ParameterEdge(baseEdge.getSource(), newTarget);
					
//					eaug.aug.addEdge(baseEdge.getSource(), newTarget, newEdge);
					edgesToAdd.add(newEdge);
					
					Edge oldEdgeToRemove = oldEdgesToRemove.get(baseEdge.getTarget());
					edgesToRemove.add(oldEdgeToRemove);
//					eaug.aug.removeEdge(oldEdgeToRemove);
				}				
			}
			
			for (Edge edge : edgesToAdd) {
				eaug.aug.addEdge(edge.getSource(), edge.getTarget(), edge);
			}
			for (Edge edge: edgesToRemove) {
				eaug.aug.removeEdge(edge);
			}
//			
			
			for (Edge edge : eaug.aug.edgeSet()) {
				BaseEdge baseEdge = ((BaseEdge) edge);
				String sourceLabel = new BaseAUGLabelProvider().getLabel(baseEdge.getSource());
				String targetLabel = new BaseAUGLabelProvider().getLabel(baseEdge.getTarget());
				String edgeType = 	new BaseAUGLabelProvider().getLabel(baseEdge);
				String edgeLabel =sourceLabel + " -> " +targetLabel + " (" + edgeType + ")";
				expressionStart.put(edgeLabel, -1); // dummy positions, we just want to track the edges somehow
				expressionEnd.put(edgeLabel, -1);
				expressionStartLine.put(edgeLabel, -1);
			}
			
			
			Map<String, Object> json = new HashMap<>();
			json.put("expressionStart", expressionStart);
			json.put("expressionStartAdditional", expressionStartAdditional);
			json.put("expressionEndAdditional", expressionEndAdditional);
			json.put("expressionEnd", expressionEnd);
			json.put("expressionStartLine", expressionStartLine);
			json.put("graphId", instanceId);
			
			json.put("rawCode", ((APIUsageExample) eaug.aug).sourceCode.substring(methodDeclaration.getStartPosition(), methodDeclaration.getStartPosition() + methodDeclaration.getLength()));
			json.put("rawCodeLineNumbers", ((APIUsageExample) eaug.aug).sourceCode.substring(0, methodDeclaration.getStartPosition()).split("\n").length);
//			json.put("linenum", );
			
			
			if (startAndEndJsons != null) {
				startAndEndJsons.put(i, new Gson().toJson(json));
			}
			
			//

			Map<Node, Integer> vertexNumbers = new HashMap<>();
			
			int nodeNumber = 0;
			nodeNumber = writeNodesInAug(vertexLabels, writer, aug, vertexNumbers, nodeNumber);
			
			//
			writeEdgesInAug(edgeLabels, writer, aug, vertexNumbers, vertexLabels);
			
			writer.write("-\n");
			
			i++;
			instanceId ++;
		}
		
		System.out.println("i is " + i);
		return i;
	}

	private static void initEdgeLabels(Map<String, Integer> edgeLabels) {
//		edgeLabels.put("throw", 0);
//		edgeLabels.put("def", 1);
//		edgeLabels.put("hdl", 2);
//		edgeLabels.put("contains", 3);
//		edgeLabels.put("recv", 4);
//		edgeLabels.put("finally", 5);
//		edgeLabels.put("sel", 6);
//		edgeLabels.put("rep", 7);
//		edgeLabels.put("sync", 8);
//		edgeLabels.put("para", 9);
//		edgeLabels.put("order", 10);
		
//		edgeLabels.put("order_rev", 11);
		
		edgeLabels.put("throw_lower_to_higher", 0);
		edgeLabels.put("def_lower_to_higher", 1);
		edgeLabels.put("hdl_lower_to_higher", 2);
		edgeLabels.put("contains_lower_to_higher", 3);
		edgeLabels.put("recv_lower_to_higher", 4);
		edgeLabels.put("finally_lower_to_higher", 5);
		edgeLabels.put("sel_lower_to_higher", 6);
		edgeLabels.put("rep_lower_to_higher", 7);
		edgeLabels.put("sync_lower_to_higher", 8);
		edgeLabels.put("para_lower_to_higher", 9);
		edgeLabels.put("order_lower_to_higher", 10);


		edgeLabels.put("throw_higher_to_lower", 11);
		edgeLabels.put("def_higher_to_lower", 12);
		edgeLabels.put("hdl_higher_to_lower", 13);
		edgeLabels.put("contains_higher_to_lower", 14);
		edgeLabels.put("recv_higher_to_lower", 15);
		edgeLabels.put("finally_higher_to_lower", 16);
		edgeLabels.put("sel_higher_to_lower", 17);
		edgeLabels.put("rep_higher_to_lower", 18);
		edgeLabels.put("sync_higher_to_lower", 19);
		edgeLabels.put("para_higher_to_lower", 20);
		edgeLabels.put("order_higher_to_lower", 21);
	}

	private static void writeEdgesInAug(Map<String, Integer> edgeLabels, BufferedWriter writer, APIUsageGraph aug,
			Map<Node, Integer> vertexNumbers, Map<String, Integer> vertexLabels) throws IOException {
		
		writeEdgesInAug(edgeLabels, writer, aug, vertexNumbers, vertexLabels, null);
	}
	
	private static void writeEdgesInAug(Map<String, Integer> edgeLabels, BufferedWriter writer, APIUsageGraph aug,
			Map<Node, Integer> vertexNumbers, Map<String, Integer> vertexLabels, Map<Node, Node> equivalentNodeToJoinPoint) throws IOException {
	
		
		for (Edge edge : aug.edgeSet()) {
			String edgeLabel = new BaseAUGLabelProvider().getLabel(edge);
			String otherEdgeLabel = edgeLabel;
			
			
			int sourceNumber = getNodenumber(vertexNumbers, equivalentNodeToJoinPoint, edge.getSource());
			
			int targetNumber = getNodenumber(vertexNumbers, equivalentNodeToJoinPoint, edge.getTarget());
			
			// 
			
			
			String sourceNodeLabel = new BaseAUGLabelProvider().getLabel(edge.getSource());
			int sourceNodeLabelIndex = vertexLabels.get(sourceNodeLabel);
			
			String targetNodeLabel = new BaseAUGLabelProvider().getLabel(edge.getTarget());
			
			int targetNodeLabelIndex = vertexLabels.get(targetNodeLabel);
			if (targetNodeLabelIndex > sourceNodeLabelIndex) {
				edgeLabel += "_lower_to_higher";
				otherEdgeLabel += "_higher_to_lower"; 
			} else {
				edgeLabel += "_higher_to_lower";
				otherEdgeLabel += "_lower_to_higher";
			}
			
			
			if (!edgeLabels.containsKey(edgeLabel)) {
				edgeLabels.put(edgeLabel, edgeLabels.size());
			}
			
			int edgeLabelIndex = edgeLabels.get(edgeLabel);	
			int otherEdgeLabelIndex = edgeLabels.get(otherEdgeLabel);
	
			writer.write("e " + sourceNumber + " " + targetNumber + " " + edgeLabelIndex      + "\n");
			
			writer.write("e " + targetNumber + " " + sourceNumber + " " + otherEdgeLabelIndex + "\n");
			
			
//			if (edgeLabel.equals("order")) {
//				edgeLabelIndex = edgeLabels.get("order_rev");	
//				sourceNumber = getNodenumber(vertexNumbers, equivalentNodeToJoinPoint, (edge.getTarget()));
//				targetNumber = getNodenumber(vertexNumbers, equivalentNodeToJoinPoint, (edge.getSource()));
//				
//				writer.write("e " + sourceNumber + " " + targetNumber + " " + edgeLabelIndex+ "\n");
//				
//			}
		}
	}

	private static int getNodenumber(Map<Node, Integer> vertexNumbers, Map<Node, Node> equivalentNodeToJoinPoint,
			Node source) {
		if (equivalentNodeToJoinPoint == null) {
			return vertexNumbers.get(source);
		}
		
		int number;
		if (equivalentNodeToJoinPoint.containsKey(source)) {
			if (!vertexNumbers.containsKey(equivalentNodeToJoinPoint.get(source))) {
				throw new RuntimeException("Uhhh This shouldn't happen! " + source.getId());
			}
			number = vertexNumbers.get( 
					equivalentNodeToJoinPoint.get(source)
					);
		} else {
			number = vertexNumbers.get(source);
		}
		return number;
	}

	private static int writeNodesInAug(Map<String, Integer> vertexLabels, BufferedWriter writer, APIUsageGraph aug,
			Map<Node, Integer> vertexNumbers, int nodeNumber) throws IOException {
		 return writeNodesInAug(vertexLabels, writer, aug, vertexNumbers, nodeNumber, null,  null);
		
	}
	
	private static int writeNodesInAug(Map<String, Integer> vertexLabels, BufferedWriter writer, APIUsageGraph aug,
			Map<Node, Integer> vertexNumbers, int nodeNumber, Node nodeToReplace, Node nodeToJoinAt) throws IOException {
		for (Node vertex : aug.vertexSet()) {
			if (vertex != null && nodeToJoinAt != null && vertex == nodeToReplace) {
				continue;
			}
			
			 String nodeLabel = new BaseAUGLabelProvider().getLabel(vertex);
			 
			 if (!vertexLabels.containsKey(nodeLabel)) {
				 vertexLabels.put(nodeLabel, vertexLabels.size());	 
			 }
			 int nodeLabelIndex = vertexLabels.get(nodeLabel);
			 
			 writer.write("v " + nodeNumber + " " + nodeLabelIndex+ "\n");
//			 System.out.println("v " + nodeNumber + " " + nodeLabelIndex);
//			 System.out.println("v was " + nodeLabel);
			 
			 vertexNumbers.put(vertex, nodeNumber);
			 
			 nodeNumber+=1;
		}
		
		return nodeNumber;
	}
	
}
