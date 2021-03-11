package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.SyncRule;
import co.com.bancolombia.binstash.model.SyncType;
import com.google.re2j.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluatorUseCaseTest {

    @Test
    @DisplayName("Create instance")
    void testCreate() {
        RuleEvaluatorUseCase emptyREV = new RuleEvaluatorUseCase(null);
        assertEquals(1, emptyREV.getRules().length);

        SyncRule r1 = (keyArg, syncType) -> true;
        RuleEvaluatorUseCase singleREV = new RuleEvaluatorUseCase(Collections.singletonList(r1));
        assertEquals(1, singleREV.getRules().length);
    }

    @Test
    @DisplayName("Evaluate simple expression")
    void testEvaluateExpression() {

        SyncRule r1 = (keyArg, syncType) -> keyArg.startsWith("some") && syncType == SyncType.DOWNSTREAM;

        RuleEvaluatorUseCase rev = new RuleEvaluatorUseCase(Collections.singletonList(
                r1
        ));

        assertTrue(rev.evalForDownstreamSync("some:key"));

        assertTrue(rev.evalForDownstreamSync("some_key@at|mem"));

        assertFalse(rev.evalForDownstreamSync(""));

        assertFalse(rev.evalForDownstreamSync(null));

        assertFalse(rev.evalForUpstreamSync("some:key"));

    }

    @Test
    @DisplayName("Evaluate key beginning expression")
    void testEvaluateBegExpression() {

        final Pattern pattern = Pattern.compile("^d2b\\S+");
        final SyncRule r1 = (keyArg, syncType) -> {
            if (StringUtils.isBlank(keyArg))
                return false;
            return pattern.matches(keyArg);
        };

        RuleEvaluatorUseCase rev = new RuleEvaluatorUseCase(Collections.singletonList(
                r1
        ));

        assertFalse(rev.evalForDownstreamSync("some:key"));

        assertTrue(rev.evalForDownstreamSync("d2b-ms1-some-key"));

        assertFalse(rev.evalForDownstreamSync(""));

        assertFalse(rev.evalForDownstreamSync(null));
    }

}
