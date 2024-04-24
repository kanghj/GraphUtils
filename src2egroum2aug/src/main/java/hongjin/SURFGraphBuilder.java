package hongjin;

import static hongjin.EAUGUtils.buildAUGsForClassFromSomewhereElse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

import edu.iastate.cs.egroum.aug.AUGConfiguration;
import edu.iastate.cs.egroum.aug.LiberalTextMatchUsageExamplePredicate;
import edu.iastate.cs.egroum.aug.TextMatchUsageExamplePredicate;
import edu.iastate.cs.egroum.aug.UsageExamplePredicate;
import smu.hongjin.EnhancedAUG;
import smu.hongjin.LiteralsUtils;

public class SURFGraphBuilder {

	public static void main(String... args) throws IOException {
//		HJConstants.directoriesToExamplesOfAPI.put("javax.crypto.Cipher__init",
//				new HashSet<>(Arrays.asList("/Users/hongjinkang/repos/active_learning_interface/Surf/code/meteor_app/full_source/cryptoapi_bench_Cipher"))
//				);
//		HJConstants.directoriesToExamplesOfAPI.put("java.security.MessageDigest__digest",
//				new HashSet<>(Arrays.asList("/Users/hongjinkang/repos/active_learning_interface/Examplore/code/meteor_app/full_source/cryptoapi_bench_MessageDigest"))
//				);
//		HJConstants.directoriesToExamplesOfAPI.put("java.security.SecureRandom__next",
//				new HashSet<>(Arrays.asList("/Users/hongjinkang/repos/active_learning_interface/Examplore/code/meteor_app/full_source/cryptoapi_bench_SecureRandom"))
//				);
		SURFGraphBuilder.buildGraphs(args[0], args[1], args[2]);
	}

	static Map<File, String> fileContents = new HashMap<>();

	public static void buildGraphs(String API, String directory, String metaDataDirectory) throws IOException {


		Map<String, String> labels = new HashMap<>();
		Map<String, Integer> map1 = new HashMap<>();
		Map<String, Integer> map2 = new HashMap<>();

		Map<Integer, String> startAndEndJsons = new HashMap<>();

		// read vertmap
		String vertMapDirectory = metaDataDirectory + API + "_vertmap.txt";
		List<String> lines = Files.readAllLines(Paths.get(vertMapDirectory));
		for (String line : lines) {
			int lastIndex = line.lastIndexOf(",");
			// System.out.println(line);
			String token = line.substring(0, lastIndex);
			String countAsStr = line.substring(lastIndex + 1);
			map1.put(token, Integer.parseInt(countAsStr));
		}

		String edgeMapDirectory = metaDataDirectory + API + "_edgemap.txt";
		lines = Files.readAllLines(Paths.get(edgeMapDirectory));
		for (String line : lines) {
			String[] splitted = line.split(",");
			map2.put(splitted[0], Integer.parseInt(splitted[1]));
		}
		
		int count = 0;
		Collection<File> pathsToJavaFiles = FileUtils.listFiles(new File(directory), new String[] { "java" }, true);

		try (BufferedWriter writer = new BufferedWriter(
				new FileWriter(metaDataDirectory + API + "_formatted.txt"));
				BufferedWriter idMappingWriter = new BufferedWriter(
						new FileWriter(metaDataDirectory + API + "_graph_id_mapping.txt"));
				BufferedWriter elementPositionWriter = new BufferedWriter(
						new FileWriter(metaDataDirectory + API + "_elementpositions.json"))) {

			for (File javaFile : pathsToJavaFiles) {
				String pathToJavaFile = javaFile.toString();
				String code;
				if (!fileContents.containsKey(javaFile)) {
					code = new String(Files.readAllBytes(new File(pathToJavaFile).toPath()));
				} else {
					code = fileContents.get(javaFile);
				}

				UsageExamplePredicate predicate;
				if (!API.equals("java.security.SecureRandom__next")) {
					predicate = TextMatchUsageExamplePredicate.TextMatchUsageExampleOf(
							GraphBuildingUtils.APIToMethodName.get(API).iterator().next(),
							GraphBuildingUtils.APIToClass.get(API));
				} else {
					// for SecureRandom/Random, match any example containing next*
					predicate = LiberalTextMatchUsageExamplePredicate.TextMatchUsageExampleOf(
							GraphBuildingUtils.APIToMethodName.get(API).iterator().next());
				}
				Collection<EnhancedAUG> eaugs = buildAUGsForClassFromSomewhereElse(code, pathToJavaFile,
						pathToJavaFile.substring(pathToJavaFile.lastIndexOf("/")), new AUGConfiguration() {
							{
								usageExamplePredicate = predicate;
//								usageExamplePredicate = UsageExamplePredicate.allUsageExamples();
							}
						}, null);

				System.out.println("\tFinished building some eaugs!");

				int instanceId = count;
				for (EnhancedAUG eaug : eaugs) {
					String labelId = instanceId + " - " + eaug.aug.name;
					labels.put(labelId, "U");
					instanceId ++;
				}

				LiteralsUtils.keepAllLiterals = true;
				count = SubgraphMiningFormatter.convert(eaugs, count, map1, map2, count , labels, 1, "",
						writer, idMappingWriter, startAndEndJsons);

				System.out.println("will write to " + metaDataDirectory + API + "_elementpositions.json");
				System.out.println("length of startAndEndJsons " + startAndEndJsons.size());
				
				

			}
			elementPositionWriter.write(new Gson().toJson(startAndEndJsons));
			
		}
		System.out.println("will write to  " + metaDataDirectory + API + "_vertmap.txt");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(metaDataDirectory + API + "_vertmap.txt"))) {
			for (Entry<String, Integer> entry1 : map1.entrySet()) {
				writer.write(entry1.getKey().trim() + "," + entry1.getValue() + "\n");
			}
		}
		System.out.println("will write to " + metaDataDirectory + API + "_edgemap.txt");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(metaDataDirectory + API + "_edgemap.txt"))) {
			for (Entry<String, Integer> entry1 : map2.entrySet()) {
				writer.write(entry1.getKey().trim() + "," + entry1.getValue() + "\n");
			}
		}

	}

}
