package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.SyncRule;
import co.com.bancolombia.binstash.model.SyncType;
import com.google.re2j.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class RuleEvaluatorUseCaseTest {

    @Test
    @DisplayName("Create instance")
    public void testCreate() {
        RuleEvaluatorUseCase emptyREV = new RuleEvaluatorUseCase(null);
        assert emptyREV.getRules().length == 1;

        SyncRule r1 = (keyArg, syncType) -> true;
        RuleEvaluatorUseCase singleREV = new RuleEvaluatorUseCase(Collections.singletonList(r1));
        assert singleREV.getRules().length == 1;
    }

    @Test
    @DisplayName("Evaluate simple expression")
    public void testEvaluateExpression() {

        SyncRule r1 = (keyArg, syncType) -> keyArg.startsWith("some") && syncType == SyncType.DOWNSTREAM;

        RuleEvaluatorUseCase rev = new RuleEvaluatorUseCase(Collections.singletonList(
                r1
        ));

        assert rev.evalForDownstreamSync("some:key");

        assert rev.evalForDownstreamSync("some_key@at|mem");

        assert !rev.evalForDownstreamSync("");

        assert !rev.evalForDownstreamSync(null);

        assert !rev.evalForUpstreamSync("some:key");

    }

    @Test
    @DisplayName("Evaluate key beginning expression")
    public void testEvaluateBegExpression() {

        final Pattern pattern = Pattern.compile("^d2b\\S+");
        final SyncRule r1 = (keyArg, syncType) -> {
            if (StringUtils.isBlank(keyArg))
                return false;
            return pattern.matches(keyArg);
        };

        RuleEvaluatorUseCase rev = new RuleEvaluatorUseCase(Collections.singletonList(
                r1
        ));

        assert !rev.evalForDownstreamSync("some:key");

        assert rev.evalForDownstreamSync("d2b-ms1-some-key");

        assert !rev.evalForDownstreamSync("");

        assert !rev.evalForDownstreamSync(null);
    }

}
