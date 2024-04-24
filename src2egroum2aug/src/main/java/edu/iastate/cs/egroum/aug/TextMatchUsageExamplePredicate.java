package edu.iastate.cs.egroum.aug;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * HJ: match text
 * 
 * @author kanghongjin
 *
 */
public class TextMatchUsageExamplePredicate implements UsageExamplePredicate {

	private final Set<String> methodNames;
	private final Set<String> simpleTypeNames;
	

	public static TextMatchUsageExamplePredicate TextMatchUsageExampleOf(String methodName,
			String... typeNames) {
		if (typeNames.length == 0)
			throw new RuntimeException("wrong");
		return new TextMatchUsageExamplePredicate(methodName, typeNames);
	}


	  protected TextMatchUsageExamplePredicate(String methodName, String... typeNames) {
	        this.simpleTypeNames = new HashSet<>();
	        for (String fullyQualifiedTypeName : typeNames) {
	            simpleTypeNames.add(fullyQualifiedTypeName.substring(fullyQualifiedTypeName.lastIndexOf('.') + 1));
	        }
	        
	        methodNames = new HashSet<>();
	        methodNames.add(methodName);
	    }

	
	private static boolean debug = false;
	@Override
	public boolean matches(String sourceFilePath, CompilationUnit cu) {
		containing = false;
		return matches(cu);
	}

	@Override
	public boolean matches(MethodDeclaration methodDeclaration) {
		containing = false;
	
		return matches((ASTNode) methodDeclaration);
	}

	private boolean containing;

	private boolean matches(ASTNode node) {
		if (matchesAnyExample())
			return true;

		return node.toString().contains(methodNames.iterator().next());
	}

	@Override
	public boolean matches(EGroumGraph graph) {
        return matchesAnyExample() || !Collections.disjoint(graph.getAPIs(), simpleTypeNames);
    }

	private boolean matchesAnyExample() {
		return methodNames.isEmpty();
	}
}
