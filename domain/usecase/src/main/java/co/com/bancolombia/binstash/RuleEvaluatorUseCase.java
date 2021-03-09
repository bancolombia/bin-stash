package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.SyncRule;
import co.com.bancolombia.binstash.model.SyncType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RuleEvaluatorUseCase {

    private static final SyncRule DEFAULT_PERMISSIVE_SYNC_RULE = (keyArg, syncType) -> true;

    private final List<SyncRule> syncRules;

    public RuleEvaluatorUseCase(List<SyncRule> syncRules) {
        this.syncRules = new ArrayList<>();
        if (syncRules == null || syncRules.isEmpty()) {
            // Add a default route. Push local cache key-values to distributed cache (upstream = true)
            // and pull distributed cache key-values from distributed cache (downstream = true)
            // when affected key is ANY string.
            this.syncRules.add(DEFAULT_PERMISSIVE_SYNC_RULE);
        } else {
            this.syncRules.addAll(syncRules);
        }
    }

    public boolean evalForUpstreamSync(String key) {
        return evalFor(key, SyncType.UPSTREAM);
    }

    public boolean evalForDownstreamSync(String key) {
        return evalFor(key, SyncType.DOWNSTREAM);
    }

    private boolean evalFor(String key, SyncType syncType) {
        if (StringUtils.isBlank(key))
            return false;
        return this.syncRules.stream()
                .anyMatch(syncRule -> syncRule.apply(key, syncType));
    }

    public SyncRule[] getRules() {
        SyncRule[] response = new SyncRule[this.syncRules.size()];
        this.syncRules.toArray(response);
        return response;
    }

}
