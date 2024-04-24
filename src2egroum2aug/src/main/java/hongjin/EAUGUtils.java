package hongjin;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import edu.iastate.cs.egroum.aug.AUGConfiguration;
import smu.hongjin.EnhancedAUG;

public class EAUGUtils {
    public static Collection<EnhancedAUG> buildAUGsForClassFromSomewhereElse(String classCode, String elseWhere, String project, AUGConfiguration configuration, String[] classpath) {
        AUGBuilder builder = new AUGBuilder(configuration);
        String basePath = elseWhere;
        Collection<APIUsageExample> aug = builder.build(classCode, basePath, project, classpath);
        return EnhancedAUG.buildEnhancedAugs(new HashSet<>(aug));
    }
    
    public static Collection<EnhancedAUG> buildAUGsForClassFromSomewhereElse(String classCode, String elseWhere, String project, AUGConfiguration configuration) {
        AUGBuilder builder = new AUGBuilder(configuration);
        String basePath = elseWhere;
        Collection<APIUsageExample> aug = builder.build(classCode, basePath, project, null);
        System.out.println(" converting AUG to EAUG. #augs = " + aug.size());
        return EnhancedAUG.buildEnhancedAugs(new HashSet<>(aug));
    }
    
	static int count = 0;

	public static boolean isTooBig(MethodDeclaration md) {
		count = 0;
		md.accept(new ASTVisitor(false) {
			@Override
			public boolean preVisit2(ASTNode node) {
				if (node instanceof Statement) {
					count++;
				}
				return true;
			}
		});
		return count > new AUGConfiguration().maxStatements;
	}
}
