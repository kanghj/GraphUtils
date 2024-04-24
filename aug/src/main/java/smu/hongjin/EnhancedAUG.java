package smu.hongjin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageGraph;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;

/**
 * Wraps over an AUG + stores other information
 * We should revisit the design of this class at some point since we started storing other information elsewhere.
 */
public class EnhancedAUG {

	public APIUsageGraph aug;
	
	Set<APIUsageGraph> related = new HashSet<>();
	Set<String> interfaces = new HashSet<>();
	
	public EnhancedAUG(APIUsageGraph aug, Set<APIUsageGraph>  related, Set<String> interfaces) {
		this.aug = aug;
		this.related = related;
		this.interfaces = interfaces;
	}
	
	public static Set<EnhancedAUG> buildEnhancedAugs(Set<APIUsageGraph> augs) {
		
		Map<String, APIUsageGraph> fieldInit = new HashMap<>();
		
		
		for (APIUsageGraph aug : augs) {
			if (aug instanceof APIUsageExample) {
				boolean isCtor = aug.isCtor;
				if (!isCtor) continue;
				
				for (Entry<String, Node> entry : aug.fieldsUsed.entrySet()) {
					String field = entry.getKey();
					
					fieldInit.put(field, aug);
				}
			}
		}
		
		Set<EnhancedAUG> result = new HashSet<>();
		Iterator<APIUsageGraph> iter = augs.iterator();
		
		
		while (iter.hasNext()) {
			APIUsageGraph aug = iter.next();
			
			Map<String, Node> fieldsUsed = aug.fieldsUsed;
			Set<APIUsageGraph> relat = new HashSet<>();
			for ( Entry<String, Node> entry : fieldsUsed.entrySet()) {
				String field = entry.getKey();
				if (!fieldInit.containsKey(field)) continue;
				relat.add(fieldInit.get(field));
				
			}
		
			System.out.println("building one eaug");
			
			result.add(new EnhancedAUG(aug, relat, aug.interfaces));
		}
		
		return result;
	}
	
}
