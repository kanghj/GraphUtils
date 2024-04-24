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
public class AnyTextMatchUsageExamplePredicate implements UsageExamplePredicate {

	private final Set<String> texts;
	

	public static AnyTextMatchUsageExamplePredicate TextMatchUsageExampleOf(String... text) {
		Set<String> inputs = new HashSet<>();
        for (String oneText : text) {
            inputs.add(oneText);
        }
		
		return new AnyTextMatchUsageExamplePredicate(inputs);
	}

	
	  protected AnyTextMatchUsageExamplePredicate(Set<String> texts) {
		  this.texts = texts;
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

		return texts.stream().anyMatch(text -> {
			return node.toString().contains(text);
		});
		
	}

	@Override
	public boolean matches(EGroumGraph graph) {
        return true; 
    }

	private boolean matchesAnyExample() {
		return texts.isEmpty();
	}
}
