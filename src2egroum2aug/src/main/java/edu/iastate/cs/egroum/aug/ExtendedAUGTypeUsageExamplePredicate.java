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
 * HJ: same as TypeUsageExamplePredicate.java but! we check the method name too,
 * instead of only checking the type
 * 
 * @author kanghongjin
 *
 */
public class ExtendedAUGTypeUsageExamplePredicate implements UsageExamplePredicate {
	private final Set<String> fullyQualifiedTypeNames;
	private final Set<String> simpleTypeNames;

	private final Set<String> methodNames;
	private final boolean isCtor;

	public static Map<String, Boolean> relaxMatchingCriteria = new HashMap<>();
	static {
		// for names that are more identifiable, we don't have to check the types
		// strictly.
		//in some cases, we add it here because it's too annoying to gry to match things from third party libraries otherwise
		
		// it's usually ok, because we match against source  code which was already filtered using the github-code-search tool 
		relaxMatchingCriteria.put("getMeanValue", true);
		relaxMatchingCriteria.put("getPdfObject", true);
		relaxMatchingCriteria.put("info", true);
		relaxMatchingCriteria.put("getRendererForDataset", true);
		relaxMatchingCriteria.put("getOwner", true);
		relaxMatchingCriteria.put("getAsString", true);
		relaxMatchingCriteria.put("getDataset", true);
		relaxMatchingCriteria.put("isPreemptive", true);
		relaxMatchingCriteria.put("getParameter", true);
		
		relaxMatchingCriteria.put("open", true);
		relaxMatchingCriteria.put("get", true);
//		relaxMatchingCriteria.put("intersection", true);

	}
	
	public static Map<String, String> additionalJar = new HashMap<>();
	static {
		additionalJar.put("com.google.common.collect.Multimap__get__1", "/Users/kanghongjin/Downloads/guava-28.2-jre.jar");
	}

	public static ExtendedAUGTypeUsageExamplePredicate EAUGUsageExamplesOf(String methodName,
			String... fullyQualifiedTypeNames) {
		if (fullyQualifiedTypeNames.length == 0)
			throw new RuntimeException("wrong");
		return new ExtendedAUGTypeUsageExamplePredicate(Collections.singleton(methodName), fullyQualifiedTypeNames);
	}

	public static ExtendedAUGTypeUsageExamplePredicate EAUGUsageExamplesOf(Set<String> methodNames,
			String... fullyQualifiedTypeNames) {
		if (fullyQualifiedTypeNames.length == 0)
			throw new RuntimeException("wrong");
		return new ExtendedAUGTypeUsageExamplePredicate(methodNames, fullyQualifiedTypeNames);
	}

	protected ExtendedAUGTypeUsageExamplePredicate(Set<String> methodNames, String... fullyQualifiedTypeNames) {
		this.fullyQualifiedTypeNames = new HashSet<>(Arrays.asList(fullyQualifiedTypeNames));
		this.simpleTypeNames = new HashSet<>();
		for (String fullyQualifiedTypeName : fullyQualifiedTypeNames) {
			simpleTypeNames.add(fullyQualifiedTypeName.substring(fullyQualifiedTypeName.lastIndexOf('.') + 1));
		}

		this.methodNames = new HashSet<>(methodNames);

		this.isCtor = methodNames.stream().anyMatch(name -> name.contains("init>"));
	}

	
	private static boolean debug = true;
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

		if (debug) {
			System.out.println("is relaxed matching? " + ((relaxMatchingCriteria.entrySet().stream()
				.anyMatch(nameWhichCanRelax -> methodNames.contains(nameWhichCanRelax.getKey())))
					|| EGroumBuilder.USE_FALLBACK));
			node.accept(new ASTVisitor(false) {
				@Override
				public boolean visit(MethodInvocation node) {
					return !isDeclaredByApiClass(node.getName(), false) && super.visit(node);
				}
				
				private boolean isDeclaredByApiClass(SimpleName name, boolean b) {
					
					System.out.println("name found during parse = " + name.toString() + " vs names expected=" + methodNames + " is match?" + (methodNames.contains(name.toString())));
					
					return false;
				}


				@Override
				public boolean preVisit2(ASTNode node) {
					return !containing && super.preVisit2(node);
				}
			});
		}
		
		if (relaxMatchingCriteria.entrySet().stream()
				.anyMatch(nameWhichCanRelax -> methodNames.contains(nameWhichCanRelax.getKey()))
				|| EGroumBuilder.USE_FALLBACK) { // USE_FALLBACK: it makes sense to use a relaxed matching: the graph label will be missing anyway, even if matching spuriously, which will cause the spurious matches to be filtered
			
			containing = containing || false;
			// a less strict visitor
			node.accept(new ASTVisitor(false) {
				@Override
				public boolean visit(MethodInvocation methodInvocationNode) {
				
					if (isCtor && EGroumBuilder.USE_FALLBACK) { // fallback to a crappy check, eclipse doesn't seem to know the type where the method was declared(?), but this means I simply resort to a string check 
						System.out.println("checking source = " + node.toString());
						for (String s : simpleTypeNames) {
							System.out.println("\tagainst s = " + s);
							if (node.toString().contains(s)) {
								System.out.println("return true since node contains " + s);
								containing = true;
								return true;
							}
						}
					}
					
					// hardcode to get around some difficulties with relaxed matching and names that are matched too easily
					boolean isCommonName = methodNames.contains("next") || methodNames.contains("close") 
							|| methodNames.contains("init") || methodNames.contains("get") 
							|| methodNames.contains("write") || methodNames.contains("info")
							|| methodNames.contains("getParameter");
					if (isCommonName && EGroumBuilder.USE_FALLBACK) {
						boolean hasTypeMatch = false;
						for (String s : simpleTypeNames) {
							if (node.toString().toLowerCase().contains(s.toLowerCase())) {
								hasTypeMatch = true;
							}
						}
						if (!hasTypeMatch) {
							// no need to check, otherwise there may be many false positive matches
							System.out.println("missing reference to type " + simpleTypeNames);
							return false;
						}
					}
					
					return !isDeclaredByApiClass(methodInvocationNode.getName(), false) && super.visit(methodInvocationNode);
				}
	
				
				private boolean isDeclaredByApiClass(SimpleName name, boolean b) {
					
					  
					if (methodNames.contains(name.toString()) ) {
						containing = true;
						if (debug) {
							System.out.println("===\tSet `containing` to true!");
						}
						return true;
					}
					
					
					return false;
				}


				@Override
				public boolean preVisit2(ASTNode node) {
					return !containing && super.preVisit2(node);
				}
			});
			
		} else {
			containing = containing || false;
			node.accept(new ASTVisitor(false) {
				@Override
				public boolean visit(MethodInvocation node) {
					return !isDeclaredByApiClass(node.resolveMethodBinding(), false) && super.visit(node);
				}

				@Override
				public boolean visit(ConstructorInvocation node) {
					return !isDeclaredByApiClass(node.resolveConstructorBinding(), true) && super.visit(node);
				}

				@Override
				public boolean visit(ClassInstanceCreation node) {
					return !isDeclaredByApiClass(node.resolveConstructorBinding(), true) && super.visit(node);
				}

				private boolean isDeclaredByApiClass(IMethodBinding mb, boolean isCtor2) {
					if (isCtor2 && isCtor && !EGroumBuilder.USE_FALLBACK) {
						containing = true;
						return true;
					}

					if (mb != null) {
						if (!methodNames.contains(mb.getName())) {
							return false;
						}

						Set<String> names = new HashSet<>();

						Stack<ITypeBinding> traversing = new Stack<>();
						traversing.add(mb.getDeclaringClass().getTypeDeclaration());

						while (!traversing.isEmpty()) {
							ITypeBinding current = traversing.pop();

							if (current.getSuperclass() != null) {
								traversing.add(current.getSuperclass());
							}
							for (ITypeBinding interfaceObj : current.getInterfaces()) {

								traversing.add(interfaceObj);
							}

							if (!current.getQualifiedName().contains("<")) {
								names.add(current.getQualifiedName());
							} else {
								// we ignore things in the diamond operator, e.g. <K, T> etc..
								names.add(current.getQualifiedName().split("<")[0]);
							}
						}

						names.retainAll(fullyQualifiedTypeNames);
						if (!names.isEmpty()) {
							containing = true;
							return true;
						}
					}
					return false;
				}

				@Override
				public boolean preVisit2(ASTNode node) {
					return !containing && super.preVisit2(node);
				}
			});
		}
		return containing;
	}

	@Override
	public boolean matches(EGroumGraph graph) {
		if (relaxMatchingCriteria.entrySet().stream()
				.anyMatch(nameWhichCanRelax -> methodNames.contains(nameWhichCanRelax.getKey()))
				|| EGroumBuilder.USE_FALLBACK) {
			return true; // assuming that a previous `matches(*)` call already filtered out the obviously wrong examples 
		}
		return matchesAnyExample() || !Collections.disjoint(graph.getAPIs(), simpleTypeNames);
	}

	private boolean matchesAnyExample() {
		return fullyQualifiedTypeNames.isEmpty();
	}
}
