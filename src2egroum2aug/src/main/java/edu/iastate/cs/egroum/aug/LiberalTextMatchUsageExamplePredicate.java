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
public class LiberalTextMatchUsageExamplePredicate implements UsageExamplePredicate {

	private final String methodNamesPrefix;
	

	public static LiberalTextMatchUsageExamplePredicate TextMatchUsageExampleOf(String methodName) {
		
		return new LiberalTextMatchUsageExamplePredicate(methodName);
	}

	
	  protected LiberalTextMatchUsageExamplePredicate(String methodName) {
		  methodNamesPrefix = methodName;
	  }

	
	@Override
	public boolean matches(String sourceFilePath, CompilationUnit cu) {
		return matches(cu);
	}

	@Override
	public boolean matches(MethodDeclaration methodDeclaration) {
	
		return matches((ASTNode) methodDeclaration);
	}


	private boolean matches(ASTNode node) {
		if (matchesAnyExample())
			return true;

		return node.toString().contains(methodNamesPrefix);
	}

	@Override
	public boolean matches(EGroumGraph graph) {
        return true; // lol whatever
    }

	private boolean matchesAnyExample() {
		return methodNamesPrefix.isBlank();
	}
}
