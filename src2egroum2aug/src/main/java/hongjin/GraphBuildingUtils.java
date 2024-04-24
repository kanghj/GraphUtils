package hongjin;

import static edu.iastate.cs.egroum.aug.TypeUsageExamplePredicate.usageExamplesOf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import edu.iastate.cs.egroum.aug.AUGConfiguration;
import smu.hongjin.EnhancedAUG;

public class GraphBuildingUtils {
	public static int i;
	public static int fileCounts;

	public static Map<String, String> APIToClass = new HashMap<>();
	public static Map<String, Set<String>> APIToMethodName = new HashMap<>();

	static {	
		// special cases
		APIToClass.put("java.util.Scanner__next__0", "java.util.Iterator"); // a scanner is really just an iterator.
		APIToClass.put("java.io.PrintWriter__write__1", "java.io.Writer"); //
		APIToClass.put("javax.swing.JFrame__setVisible__1", "java.awt.Window"); //
		APIToClass.put("java.lang.String__charAt__1", "java.lang.CharSequence"); 		
	}
	


	public static void readCounts(String directory, Map<String, Integer> quantities) {
		List<String> lines = Collections.emptyList();
		try {
			lines = Files.readAllLines(Paths.get(directory + "metadata/metadata.csv"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println("Couldn't read labels");
//			e.printStackTrace();
//			throw new RuntimeException(e);
		}
		
		String[] diectorysplitted = directory.split("/");
		String subIdentifier = diectorysplitted[diectorysplitted.length - 1];


		for (String line : lines) {
			String[] splitted = line.split(",");
			String id = splitted[0];
			int quantity = Integer.parseInt(splitted[1]);

			quantities.put(subIdentifier + id, quantity);
		}
	}

	public static void readLabels(String directory, Map<String, String> labels) {
		List<String> lines = Collections.emptyList();
		try {
			lines = Files.readAllLines(Paths.get(directory + "labels.csv"), StandardCharsets.UTF_8);
		} catch (IOException  e) {
			System.out.println("Couldn't read labels");
//			e.printStackTrace();
//			throw new RuntimeException(e);
		}

		int i = 0;
		for (String line : lines) {
			if (i == 0) {
				i += 1;
				continue;
			}
			String[] splitted = line.split(",");
			String location = splitted[0];
			String label;
			if (splitted.length < 2) {
				label = "U";
			} else {
				label = splitted[1];
			}
			if (!labels.containsKey(location) || labels.get(location).equals("U")) {
				labels.put(location, label);
			}
		}
	}

	public static Collection<EnhancedAUG> buildAUGsForClassFromSomewhereElse(String classCode, String elseWhere,
			String project, AUGConfiguration configuration) {
		AUGBuilder builder = new AUGBuilder(configuration);
		String basePath = elseWhere;
		Collection<APIUsageExample> aug = builder.build(classCode, basePath, project, null);
		return EnhancedAUG.buildEnhancedAugs(new HashSet<>(aug));
	}
}
