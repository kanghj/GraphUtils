package hongjin;

import static hongjin.EAUGUtils.buildAUGsForClassFromSomewhereElse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.Gson;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import edu.iastate.cs.egroum.aug.AUGConfiguration;
import edu.iastate.cs.egroum.aug.UsageExamplePredicate;
import edu.iastate.cs.egroum.utils.JavaASTUtil;
import smu.hongjin.EnhancedAUG;
import smu.hongjin.LiteralsUtils;

public class GraphBuilderForSpotbugsSuppression {
	private static int i;
	static int fileCounts = 0;
	
	public static void main(String... args) throws IOException {
		String basePath = "/Users/hongjinkang/repos/suppression_interface/sourceDirectories/lucene-solr/";
//		
		File file = new File("/Users/hongjinkang/repos/suppression_interface/Surf/code/spotbugs_warnings_apache_lucene-solr__NULL_.json");
//		

		// read json
		String content = new String(Files.readAllBytes(file.toPath()));
		List<Object> list = new Gson().fromJson(content, List.class);

		List<String> examples = new ArrayList<>();
		List<String> methodNames = new ArrayList<>();
		List<Integer> exampleIds  = new ArrayList<>();
		for (Object warning : list) {
			Map<String, Object> mapWarning = (Map<String, Object>) warning;
			String filePath = (String) mapWarning.get("filepath");
			String targetMethod = (String) mapWarning.get("methodName");
			
			// if contains a space, we are dealing with the old format 
			if (targetMethod.contains(" ")) {
				targetMethod = targetMethod.split(" ")[1];
			}
			
			int exampleID = ( (Double) mapWarning.get("exampleID")).intValue();
//			int exampleID = 
			
			System.out.println(basePath + filePath);
			examples.add(basePath + filePath);
			methodNames.add(targetMethod);
			exampleIds.add(exampleID);
			
			for (int dummyIter = 0; dummyIter < 1; dummyIter++) {

				examples.add("dummy");
				methodNames.add("dummy");
				exampleIds.add(exampleID + 1);
				
			}

		}


		GraphBuilderForSpotbugsSuppression.buildGraphs("apache_lucene-solr__NULL_", examples, methodNames, exampleIds);
	}
	

	public static void buildGraphs(String warningTypeAndRepo, List<String> filepaths, List<String> methodNames, List<Integer> exampleIds) throws IOException {
		

			i = 0;
			fileCounts = 0;

			Map<String, String> labels = new HashMap<>();
			Map<String, Integer> map1 = new HashMap<>();
			Map<String, Integer> map2 = new HashMap<>();
			
			Map<String, List<String>> subtypingAncestry = new HashMap<>();
			
			Map<String, String> packages = new HashMap<>();
			Map<String, String> fieldsUsed = new HashMap<>();
			Map<String, String> returnType = new HashMap<>();
			
			Map<Integer, String> startAndEndJsons = new HashMap<>();

			String APIDirectory = "./output/" + warningTypeAndRepo + "/";
			new File(APIDirectory).mkdirs();

			
//			for (String example : filepaths) {
			for (int i = 0; i < filepaths.size(); i++) {
				
				String example = filepaths.get(i);
				String methodName = methodNames.get(i);


				if (!new File(example).exists()) continue;

//				System.out.println("example " + example);
				try (Stream<Path> paths = Files.walk(Paths.get(example))) {
					paths.filter(Files::isRegularFile).forEach(path -> {
						if (!isExpectedJavaSourceFileFromRightSubdirectory(path)) {
							return;
						}
						try {
							String code = new String(Files.readAllBytes(path));

							String filePath = path.toFile().toString();

							
							CompilationUnit cu = (CompilationUnit) JavaASTUtil.parseSource(code, filePath,
									filePath.substring(filePath.lastIndexOf("/")), null);
							cu.accept(new ASTVisitor(false) {
								@Override
								public boolean preVisit2(ASTNode node) {
									if (node.getNodeType() == ASTNode.STRING_LITERAL) {
										StringLiteral strLiteral = (StringLiteral) node;
										LiteralsUtils.increaseFreq(strLiteral.getLiteralValue().replaceAll("\n", " "));
									} else if (node.getNodeType() == ASTNode.NUMBER_LITERAL) {
										NumberLiteral numLiteral = (NumberLiteral) node;
										LiteralsUtils.increaseFreq(numLiteral.getToken());
									}

									return true;
								}
							});

						} catch (IOException e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						} catch (NullPointerException npe) {
							npe.printStackTrace();
						}

					});
				}

				System.out.println("done first pass to count literals");

			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(APIDirectory + warningTypeAndRepo + "_formatted.txt"));
					BufferedWriter idMappingWriter = new BufferedWriter(
							new FileWriter(APIDirectory + warningTypeAndRepo + "_graph_id_mapping.txt"))) {
				Set<String> alreadySeen = new HashSet<>();
				
				for (int location_i = 0; location_i < filepaths.size(); location_i++) {
					
					String example = filepaths.get(location_i);
					String methodName = methodNames.get(location_i);
					
					int exampleID = exampleIds.get(location_i);
	
					String[] splitted = example.split("/");
					String subIdentifier = splitted[splitted.length - 1];


					if (!new File(example).exists()) continue;
					
					
					try (Stream<Path> paths = Files.walk(Paths.get(example))) {
						paths.filter(Files::isRegularFile).forEach(path -> {
							if (!isExpectedJavaSourceFileFromRightSubdirectory(path)) {
								return;
							}

							System.out.println("path is " + path);
							System.out.println(path.toString());
							
							if (fileCounts % 50 == 0) {
								System.out.println("count is " + fileCounts);
							}
							fileCounts += 1;

							
							int id = exampleID;
							int quantity =1;
						
					
							

							try {
								String code = new String(Files.readAllBytes(path));
								
								
								UsageExamplePredicate predicate;
								
								predicate = UsageExamplePredicate.allUsageExamples();
									
							
								String filePath = path.toFile().toString();
								Collection<EnhancedAUG> eaugs = buildAUGsForClassFromSomewhereElse(code, filePath,
										filePath.substring(filePath.lastIndexOf("/")), new AUGConfiguration() {
											{
												usageExamplePredicate = predicate;
//												usageExamplePredicate = UsageExamplePredicate.allUsageExamples();
											}
										});
								System.out.println("\tDone");
								
								eaugs.stream().forEach(eaug -> System.out.println("built aug for "  + eaug.aug.name));
								
								System.out.println("methodName ... " + methodName);
//								String[] splitedMethodName = methodName.split("\\(")[0].split("\\.");
//								System.out.println("splitedMethodName ... " + Arrays.toString(methodName));
								String targetMethodName;
								
//								System.out.println("targetMethodName ... " + targetMethodName);
								// ctor
								if (methodName.contains("<init>")) {
									targetMethodName = methodName.split("\\.")[0] + "."  + methodName.split("\\.")[0];
								}

								// arguments
								List<String> arguments = new ArrayList<>();
								
								if (methodName.contains("(") && !methodName.contains("()")) {
									for (String arg : methodName.split("\\(")[1].split("\\)")[0].split(",")) {
										arguments.add(arg.trim());
									}
								}
								
								System.out.println("arguments ... " + arguments);
								// use simple name, rather than fully qualified name
								arguments = arguments.stream().map(arg -> {
									String[] splittedArg = arg.split("\\.");
									return splittedArg[splittedArg.length - 1];
								}).toList();
								String jointArguments = String.join("#", arguments);
								
//								anonymous and inner classes are handled weirdly 
								targetMethodName = methodName.replaceAll("\\$", ".");
								
								String targetNameWithArgs= methodName + "#" + jointArguments;
								// find the right aug
								System.out.println("matching ... " +targetNameWithArgs );
								
								eaugs = eaugs.stream()
										.filter(eaug -> eaug.aug.name.contains(targetNameWithArgs))
										.toList();
								
								eaugs.stream().forEach(eaug -> System.out.println("matched!!! " + eaug.aug.name));
								if (eaugs.isEmpty()) {
//									throw new RuntimeException("arghh");
									System.out.println("failed to find aug for " +methodName );
								}
								

								String fileId = Integer.toString(id);

								int oldI = i;
								for (EnhancedAUG eaug : eaugs) {
									
									
									labels.put(id + " - " + eaug.aug.name, "U");
									
									
									
									try {
										List<String> ancestors = extractAncestors(
												path.toFile().toString(),
												path.toFile().toString().split("/java/")[0] + "/java/"
												);
										
										subtypingAncestry.put(id + " - " + eaug.aug.name, ancestors);
									} catch (IOException e1) {
										e1.printStackTrace();
										throw new RuntimeException(e1);
									}

									
									packages.put(id + " - " + eaug.aug.name, ((APIUsageExample)eaug.aug).cu.getPackage().getName() .toString());
									
									
									returnType.put(id + " - " + eaug.aug.name, ((APIUsageExample)eaug.aug).retType);
									
									Set<String> fieldsSet = ((APIUsageExample)eaug.aug).fieldsUsed.keySet();
									
									fieldsUsed.put(id + " - " + eaug.aug.name, String.join(",", fieldsSet));
								}
								
							
								
								
								i = SubgraphMiningFormatter.convert(eaugs, EnhancedAUG.class, i, map1, map2, fileId,
										labels, quantity, subIdentifier, writer, idMappingWriter, startAndEndJsons);
//							if (i == oldI) {
//								throw new RuntimeException("'i' should be increased i=" + i);
//							}
								for (int jj = oldI; jj < i; jj++) {
									String jsonString = startAndEndJsons.get(jj);
									Map things = new Gson().fromJson(jsonString, Map.class);
									if (alreadySeen.contains(things.get("rawCode"))) {
//										System.out.println("duplicate code: " +  fileId );
//										return;
									}
									alreadySeen.add((String) things.get("rawCode"));
								}
								

								
							} catch (NullPointerException npe) {
								System.out.println("NPE on " + path);
								npe.printStackTrace();
								System.err.println("err on " + path);
							} catch (Exception e) {
								System.out.println("err on " + path);
								throw new RuntimeException(e);
							}
							
							
						});
					}
				}

			}
			System.out.println("will write to  " + APIDirectory + warningTypeAndRepo + "_formatted.txt");
			System.out.println("will write to  " + APIDirectory + warningTypeAndRepo + "_vertmap.txt");
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(APIDirectory + warningTypeAndRepo + "_vertmap.txt"))) {
				for (Entry<String, Integer> entry1 : map1.entrySet()) {
					writer.write(entry1.getKey().trim() + "," + entry1.getValue() + "\n");
				}
			}
			System.out.println("will write to " + APIDirectory + warningTypeAndRepo + "_edgemap.txt");
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(APIDirectory + warningTypeAndRepo + "_edgemap.txt"))) {
				for (Entry<String, Integer> entry1 : map2.entrySet()) {
					writer.write(entry1.getKey().trim() + "," + entry1.getValue() + "\n");
				}
			}

			System.out.println("will write to " + APIDirectory + warningTypeAndRepo + "_elementpositions.json");
			System.out.println("length of startAndEndJsons " + startAndEndJsons.size());
			try (BufferedWriter writer = new BufferedWriter(
					new FileWriter(APIDirectory + warningTypeAndRepo + "_elementpositions.json"))) {
				writer.write(new Gson().toJson(startAndEndJsons));
			}
			
			
			System.out.println("will write to " + APIDirectory + warningTypeAndRepo + "_subtypingAncestry.json");
			try (BufferedWriter writer = new BufferedWriter(
					new FileWriter(APIDirectory + warningTypeAndRepo + "_subtypingAncestry.txt"))) {
				for ( Entry<String, List<String>> entry : subtypingAncestry.entrySet()) {
					writer.write(entry.getKey() + "," + String.join(",", entry.getValue()) + "\n") ;
				}
			}
			
			System.out.println("will write to " + APIDirectory + warningTypeAndRepo + "_packages.json");
			try (BufferedWriter writer = new BufferedWriter(
					new FileWriter(APIDirectory + warningTypeAndRepo + "_packages.txt"))) {
				for (Entry<String, String> entry :packages.entrySet()) {
					writer.write(entry.getKey() + "," + entry.getValue() + "\n") ;
				}
			}
			
			
			
			System.out.println("will write to " + APIDirectory + warningTypeAndRepo + "_fieldsUsed.json");
			try (BufferedWriter writer = new BufferedWriter(
					new FileWriter(APIDirectory + warningTypeAndRepo + "_fieldsUsed.txt"))) {
				for (Entry<String, String> entry :fieldsUsed.entrySet()) {
					writer.write(entry.getKey() + "," + entry.getValue() + "\n") ;
				}
			}
			
			
			System.out.println("will write to " + APIDirectory + warningTypeAndRepo + "_retType.json");
			try (BufferedWriter writer = new BufferedWriter(
					new FileWriter(APIDirectory + warningTypeAndRepo + "_retType.txt"))) {
				for (Entry<String, String> entry :returnType.entrySet()) {
					writer.write(entry.getKey() + "," + entry.getValue() + "\n") ;
				}
			}
			

		
	}

	public static boolean isExpectedJavaSourceFileFromRightSubdirectory(Path path) {
		
		if (!path.toString().contains("java")) {
			System.out.println("Skipping : " + path + ". Unexpected file extension. We only look for java files");
			return false;
		}
		return true;
	}
	
	public static List<String> extractAncestors(String filePath, String srcpath) throws IOException {
        List<String> ancestors = new ArrayList<>();

        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(srcpath));
		TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
		
	    CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(reflectionTypeSolver);
        combinedSolver.add(javaParserTypeSolver);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);
	        
		StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
		
		com.github.javaparser.ast.CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
		
		cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cid -> {
        	System.out.println(cid.resolve().getQualifiedName());
        	
        	
        	try {
	        	cid.resolve().getAllAncestors().forEach(rrt -> { 
	        		ancestors.add(rrt.describe());
	        	});
        	} catch (Exception e) {
        		System.out.println("failed to obtain ancestors for "  + cid.resolve().getQualifiedName());
        	}
	    });
        
		return ancestors;
    }
}
