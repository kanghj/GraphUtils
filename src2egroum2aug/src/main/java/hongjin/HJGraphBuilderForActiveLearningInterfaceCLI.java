package hongjin;

import static hongjin.EAUGUtils.buildAUGsForClassFromSomewhereElse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import edu.iastate.cs.egroum.aug.AUGConfiguration;
import smu.hongjin.EnhancedAUG;
import smu.hongjin.LiteralsUtils;

public class HJGraphBuilderForActiveLearningInterfaceCLI {

	public static void main(String... args) throws IOException {

		HJGraphBuilderForActiveLearningInterfaceCLI.buildGraphs(args[0], args[1], args[2], args[3]);
	}
	

	public static void buildGraphs(String API, String directory, String outputDirectory, String label) throws IOException {
		

		Map<String, Integer> quantities = new HashMap<>();

		Map<String, String> labels = new HashMap<>();
		Map<String, Integer> map1 = new HashMap<>();
		Map<String, Integer> map2 = new HashMap<>();
		
		Map<Integer, String> startAndEndJsons = new HashMap<>();
		
		String pathToJavaFile = directory + "newJavaProgram.java";
		String code = Files.readString(Paths.get(pathToJavaFile));
		
		Collection<EnhancedAUG> eaugs = buildAUGsForClassFromSomewhereElse(code, pathToJavaFile,
				pathToJavaFile.substring(pathToJavaFile.lastIndexOf("/")),
				new AUGConfiguration(),
				null);


		System.out.println("\tFinished building some eaugs!");
		for (EnhancedAUG eaug : eaugs) {
			System.out.println("\t\tFound " + eaug.aug.name);
		}
		
		// read vertmap
		String vertMapDirectory = directory + API + "_vertmap.txt";
		List<String> lines = Files.readAllLines(Paths.get(vertMapDirectory));
		for (String line : lines) {
			int lastIndex = line.lastIndexOf(",");
//			System.out.println(line);
			String token = line.substring(0, lastIndex);
			String countAsStr = line.substring(lastIndex + 1);
			map1.put(token, Integer.parseInt(countAsStr));
		}
			
		
		String edgeMapDirectory = directory + API + "_edgemap.txt";
		lines = Files.readAllLines(Paths.get(edgeMapDirectory));
		for (String line : lines) {
			String[] splitted = line.split(",");
			map2.put(splitted[0], Integer.parseInt(splitted[1]));
		}
		

		String fileId = "testFile";
		for (EnhancedAUG eaug : eaugs) {
			String labelId = fileId + " - " + eaug.aug.name;
			labels.put(labelId, label);
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(directory + API + "_test_formatted.txt"));
				BufferedWriter idMappingWriter = new BufferedWriter(new FileWriter(directory + API +"_test_graph_id_mapping.txt" ))) {
			
			LiteralsUtils.keepAllLiterals = true;
			SubgraphMiningFormatter.convert(eaugs, EnhancedAUG.class, 0, map1, map2, "testFile", labels, 1,
					"" ,writer, idMappingWriter, startAndEndJsons);

		}
		System.out.println("will write to " + directory + API + "_test_elementpositions.json");
		System.out.println("length of startAndEndJsons " + startAndEndJsons.size());
		try (BufferedWriter writer = new BufferedWriter(
				new FileWriter(directory + API + "_test_elementpositions.json"))) {
			writer.write(new Gson().toJson(startAndEndJsons));
		}



	}

}
