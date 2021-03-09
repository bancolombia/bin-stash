package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.SyncRule;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class HybridCacheFactory<V extends Object> {

    private final ObjectCache<V> localCache;
    private final ObjectCache<V> distributedCache;
    private final MapCache localMapCache;
    private final MapCache distributedMapCache;

    public ObjectCache<V> newObjectCache() {
        return this.newObjectCache(null);
    }

    public ObjectCache<V> newObjectCache(List<SyncRule> syncRules) {
        final RuleEvaluatorUseCase ruleEvaluatorUseCase = new RuleEvaluatorUseCase(syncRules);
        final DoubleTierObjectCacheUseCase<V> cache = new DoubleTierObjectCacheUseCase<>(this.localCache,
                this.distributedCache, ruleEvaluatorUseCase);
        return cache;
    }

    public MapCache newMapCache() {
        return this.newMapCache(null);
    }

    public MapCache newMapCache(List<SyncRule> syncRules) {
        final RuleEvaluatorUseCase ruleEvaluatorUseCase = new RuleEvaluatorUseCase(syncRules);
        final DoubleTierMapCacheUseCase cache = new DoubleTierMapCacheUseCase(this.localMapCache,
                this.distributedMapCache, ruleEvaluatorUseCase);
        return cache;
    }
}
