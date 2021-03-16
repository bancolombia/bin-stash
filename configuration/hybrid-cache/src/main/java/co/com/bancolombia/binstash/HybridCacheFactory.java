package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.SyncRule;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class HybridCacheFactory<V extends Object> {

    private final ObjectCache<V> localCache;
    private final ObjectCache<V> centralizedCache;
    private final MapCache localMapCache;
    private final MapCache centralizedMapCache;

    public ObjectCache<V> newObjectCache() {
        return this.newObjectCache(null);
    }

    public ObjectCache<V> newObjectCache(List<SyncRule> syncRules) {
        final RuleEvaluatorUseCase ruleEvaluatorUseCase = new RuleEvaluatorUseCase(syncRules);
        return new DoubleTierObjectCacheUseCase<>(this.localCache,
                this.centralizedCache, ruleEvaluatorUseCase);
    }

    public MapCache newMapCache() {
        return this.newMapCache(null);
    }

    public MapCache newMapCache(List<SyncRule> syncRules) {
        final RuleEvaluatorUseCase ruleEvaluatorUseCase = new RuleEvaluatorUseCase(syncRules);
        return new DoubleTierMapCacheUseCase(this.localMapCache,
                this.centralizedMapCache, ruleEvaluatorUseCase);
    }
}
