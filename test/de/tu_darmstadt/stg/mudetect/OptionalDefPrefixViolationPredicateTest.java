package de.tu_darmstadt.stg.mudetect;

import de.tu_darmstadt.stg.mudetect.model.TestAUGBuilder;
import de.tu_darmstadt.stg.mudetect.model.TestOverlapBuilder;
import org.junit.Test;

import java.util.Optional;

import static de.tu_darmstadt.stg.mudetect.model.TestAUGBuilder.buildAUG;
import static de.tu_darmstadt.stg.mudetect.model.TestOverlapBuilder.buildOverlap;
import static egroum.EGroumDataEdge.Type.DEFINITION;
import static egroum.EGroumDataEdge.Type.RECEIVER;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OptionalDefPrefixViolationPredicateTest {
    @Test
    public void missingNonDefPrefixIsNoDecision() throws Exception {
        TestOverlapBuilder overlap = buildOverlap(buildAUG(), buildAUG().withActionNode("m()"));

        Optional<Boolean> decision = new OptionalDefPrefixViolationPredicate().apply(overlap.build());

        assertThat(decision, is(Optional.empty()));
    }

    @Test
    public void missingDefPrefixAndMoreIsNoDecision() throws Exception {
        TestAUGBuilder pattern = buildAUG().withActionNodes("create()", "use()", "use2()").withDataNode("Object")
                .withDataEdge("create()", DEFINITION, "Object")
                .withDataEdge("Object", RECEIVER, "use()").withDataEdge("create()", RECEIVER, "use()")
                .withDataEdge("Object", RECEIVER, "use2()").withDataEdge("create()", RECEIVER, "use2()");
        TestAUGBuilder target = buildAUG().withActionNodes("use()").withDataNode("Object")
                .withDataEdge("Object", RECEIVER, "use()");
        TestOverlapBuilder overlap = buildOverlap(target, pattern).withNodes("use()", "Object")
                .withEdge("Object", RECEIVER, "use()");

        Optional<Boolean> decision = new OptionalDefPrefixViolationPredicate().apply(overlap.build());

        assertThat(decision, is(Optional.empty()));
    }

    @Test
    public void missingDefPrefixIsNoViolation() throws Exception {
        TestAUGBuilder pattern = buildAUG().withActionNodes("create()", "use()").withDataNode("Object")
                .withDataEdge("create()", DEFINITION, "Object")
                .withDataEdge("Object", RECEIVER, "use()").withDataEdge("create()", RECEIVER, "use()");
        TestAUGBuilder target = buildAUG().withActionNodes("use()").withDataNode("Object")
                .withDataEdge("Object", RECEIVER, "use()");
        TestOverlapBuilder overlap = buildOverlap(target, pattern).withNodes("use()", "Object")
                .withEdge("Object", RECEIVER, "use()");

        Optional<Boolean> decision = new OptionalDefPrefixViolationPredicate().apply(overlap.build());

        assertThat(decision, is(Optional.of(false)));
    }
}