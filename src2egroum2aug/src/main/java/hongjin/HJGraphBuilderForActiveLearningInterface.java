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
import java.util.Arrays;
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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.Gson;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageGraph;
import de.tu_darmstadt.stg.mudetect.aug.model.BaseNode;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.visitors.BaseAUGLabelProvider;
import edu.iastate.cs.egroum.aug.AUGConfiguration;
import edu.iastate.cs.egroum.aug.LiberalTextMatchUsageExamplePredicate;
import edu.iastate.cs.egroum.aug.TextMatchUsageExamplePredicate;
import edu.iastate.cs.egroum.aug.UsageExamplePredicate;
import edu.iastate.cs.egroum.utils.JavaASTUtil;
import smu.hongjin.EnhancedAUG;
import smu.hongjin.LiteralsUtils;

public class HJGraphBuilderForActiveLearningInterface {
	private static int i;
	static int fileCounts = 0;
	
	public static void main(String... args) throws IOException {

		String targetAPI = args[0];
		String sourcesDirectory = args[1];
		
//		HJConstants.APIUnderMiner = Arrays.asList(targetAPI);
//		HJConstants.APIUnderMiner =  Arrays.asList("javax.crypto.Cipher__init");
//		HJConstants.APIUnderMiner =  Arrays.asList("java.security.MessageDigest__digest");
//		 HJConstants.APIUnderMiner =  Arrays.asList("java.security.SecureRandom__Key");
		
//		
//		HJConstants.directoriesToExamplesOfAPI.put("javax.crypto.Cipher__init",
//				new HashSet<>(Arrays.asList("/Users/hongjinkang/repos/public_al/kanghj_repo/SURF/code/meteor_app/full_source/cryptoapi_bench_Cipher"))
//				);
//		HJConstants.directoriesToExamplesOfAPI.put("java.security.MessageDigest__digest",
//				new HashSet<>(Arrays.asList("/Users/hongjinkang/repos/active_learning_interface/Surf/code/meteor_app/full_source/cryptoapi_bench_MessageDigest"))
//				);
//		HJConstants.directoriesToExamplesOfAPI.put("java.security.SecureRandom__Key",
//				new HashSet<>(Arrays.asList("/Users/hongjinkang/repos/active_learning_interface/Surf/code/meteor_app/full_source/cryptoapi_bench_SecureRandom"))
//				);

//		HJConstants.directoriesToExamplesOfAPI.put(targetAPI, new HashSet<>(Arrays.asList(sourcesDirectory)));
//		System.out.println(HJConstants.APIUnderMiner);
		
		
		HJGraphBuilderForActiveLearningInterface.buildGraphs(
				targetAPI, 
				new HashSet<>(Arrays.asList(sourcesDirectory)));
	}
	
	private static String convertPathSeparators(String path) {
		return File.separatorChar == '/'
			? path.replace('\\', '/')
			 : path.replace('/', '\\');
	}

	public static void buildGraphs(String API, Set<String> directories) throws IOException {
		
		System.out.println("Building graphs for " + API);
		System.out.println("running " + API);

		i = 0;
		fileCounts = 0;

		Map<String, Integer> quantities = new HashMap<>();

		Map<String, String> labels = new HashMap<>();
		Map<String, Integer> map1 = new HashMap<>();
		Map<String, Integer> map2 = new HashMap<>();
		
		
		Map<Integer, String> startAndEndJsons = new HashMap<>();

		String APIDirectory = "./output/" + API + "/";
		new File(APIDirectory).mkdirs();

		for (String directory : directories) {

			// read the labels
			GraphBuildingUtils.readLabels(directory, labels);

			// read metadata to know how many copies!
			GraphBuildingUtils.readCounts(directory, quantities);

			try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
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

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(APIDirectory + API + "_formatted.txt"));
				BufferedWriter idMappingWriter = new BufferedWriter(
						new FileWriter(APIDirectory + API + "_graph_id_mapping.txt"))) {
			Set<String> alreadySeen = new HashSet<>();
			
			for (String directory : directories) {

				String[] splitted = directory.split("/");
				String subIdentifier = splitted[splitted.length - 1];


				try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
					paths.filter(Files::isRegularFile).forEach(path -> {
						if (!isExpectedJavaSourceFileFromRightSubdirectory(path)) {
							return;
						}

						System.out.println("path is " + path);
						
						if (fileCounts % 50 == 0) {
							System.out.println("count is " + fileCounts);
						}
						fileCounts += 1;

						String after = path.toAbsolutePath().toString().substring(directory.length() + 1);
						String id = after.split("/")[0];

						try {
							// throw early if id is not integer
							Integer.parseInt(id);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

						int quantity;
						if (!quantities.containsKey(subIdentifier + id)) {
//								throw new RuntimeException("unknown quantity of the graph for ID = " + id);
//								return;
							quantity =1;
						} else {
							quantity = quantities.get(subIdentifier + id);
						}
						

						try {
							String code = new String(Files.readAllBytes(path));
							
							
							UsageExamplePredicate predicate;
							if (!API.contains("java.security.SecureRandom") && !API.contains("crypto__getInstance")) {
								predicate = TextMatchUsageExamplePredicate.TextMatchUsageExampleOf(
										GraphBuildingUtils.APIToMethodName.get(API).iterator().next(),
										GraphBuildingUtils.APIToClass.get(API));
							} else if (API.contains("crypto__getInstance")) {
								predicate = TextMatchUsageExamplePredicate.TextMatchUsageExampleOf(
										"getInstance", "MessageDigest", "Cipher", "SecretKeyFactory");
							}else {
								// for SecureRandom/Random, match any example containing next*
								predicate = LiberalTextMatchUsageExamplePredicate.TextMatchUsageExampleOf(
										GraphBuildingUtils.APIToMethodName.get(API).iterator().next());
							}

							String filePath = path.toFile().toString();
							Collection<EnhancedAUG> eaugs = buildAUGsForClassFromSomewhereElse(code, filePath,
									filePath.substring(filePath.lastIndexOf("/")), new AUGConfiguration() {
										{
											usageExamplePredicate = predicate;
//												usageExamplePredicate = UsageExamplePredicate.allUsageExamples();
										}
									});
							System.out.println("\tDone");

							String fileId = id;

							int oldI = i;
							for (EnhancedAUG eaug : eaugs) {
								labels.put(id + " - " + eaug.aug.name, "U");
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
									System.out.println("duplicate code: " +  fileId );
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
		System.out.println("will write to  " + APIDirectory + API + "_formatted.txt");
		System.out.println("will write to  " + APIDirectory + API + "_vertmap.txt");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(APIDirectory + API + "_vertmap.txt"))) {
			for (Entry<String, Integer> entry1 : map1.entrySet()) {
				writer.write(entry1.getKey().trim() + "," + entry1.getValue() + "\n");
			}
		}
		System.out.println("will write to " + APIDirectory + API + "_edgemap.txt");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(APIDirectory + API + "_edgemap.txt"))) {
			for (Entry<String, Integer> entry1 : map2.entrySet()) {
				writer.write(entry1.getKey().trim() + "," + entry1.getValue() + "\n");
			}
		}

		System.out.println("will write to " + APIDirectory + API + "_elementpositions.json");
		System.out.println("length of startAndEndJsons " + startAndEndJsons.size());
		try (BufferedWriter writer = new BufferedWriter(
				new FileWriter(APIDirectory + API + "_elementpositions.json"))) {
			writer.write(new Gson().toJson(startAndEndJsons));
		}

	
	}

	public static boolean isExpectedJavaSourceFileFromRightSubdirectory(Path path) {
		if (path.endsWith("labels.csv") || path.endsWith("metadata.csv") || path.endsWith("metadata_locations.csv")) {
			System.out.println("Skipping : " + path + ", which is metadata-related file");
			return false;
		}
		if (path.toString().contains("/files/")) {
			System.out.println("Skipping : " + path + ", which contains /files");
			return false;
		}
		if (path.toString().contains("/cocci_files/")) {
			System.out.println("Skipping : " + path + ", which contains /cocci_files");
			return false;
		}
		if (!path.toString().contains("java")) {

			System.out.println("Skipping : " + path + ". Unexpected file extension. We only look for java files");
			return false;
		}
		return true;
	}
	

}
