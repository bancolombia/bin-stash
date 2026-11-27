package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.MapCache;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class DoubleTierMapCacheUseCase implements MapCache {

    private final MapCache localCache;
    private final MapCache centralizedCache;
    private final RuleEvaluatorUseCase ruleEvaluatorUseCase;

    @Override
    public Mono<Map<String, String>> saveMap(String key, Map<String, String> value) {
        return saveMap(key, value, -1);
    }

    @Override
    public Mono<Map<String, String>> saveMap(String key, Map<String, String> value, int ttl) {
        return localCache.saveMap(key, value, ttl)
                .doOnSuccess(saved -> HybridSync.upstream(ruleEvaluatorUseCase, key,
                        () -> centralizedCache.existsMap(key)
                                .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                                .flatMap(absent -> centralizedCache.saveMap(key, value, ttl))));
    }

    @Override
    public Mono<String> saveMap(String key, String field, String value) {
        return saveMap(key, field, value, -1);
    }

    @Override
    public Mono<String> saveMap(String key, String field, String value, int ttl) {
        return localCache.saveMap(key, field, value, ttl)
                .doOnSuccess(saved -> HybridSync.upstream(ruleEvaluatorUseCase, key,
                        () -> centralizedCache.existsMap(key, field)
                                .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                                .flatMap(absent -> centralizedCache.saveMap(key, field, value, ttl))));
    }

    @Override
    public Mono<String> getMap(String key, String field) {
        return localCache.getMap(key, field)
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .filter(shouldFetchFromDist -> shouldFetchFromDist)
                    .flatMap(shouldFetch -> this.centralizedCache.getMap(key, field)
                        .doOnNext(next -> HybridSync.downstream(ruleEvaluatorUseCase, key,
                                () -> this.localCache.saveMap(key, field, next))))
            ));
    }

    @Override
    public Mono<Map<String, String>> getMap(String key) {
        return localCache.getMap(key)
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .filter(shouldFetchFromDist -> shouldFetchFromDist)
                    .flatMap(shouldFetch -> this.centralizedCache.getMap(key)
                        .doOnNext(next -> HybridSync.downstream(ruleEvaluatorUseCase, key,
                                () -> this.localCache.saveMap(key, next))))
            ));
    }

    @Override
    public Mono<Boolean> existsMap(String key) {
        return localCache.existsMap(key);
    }

    @Override
    public Mono<Boolean> existsMap(String key, String field) {
        return localCache.existsMap(key, field);
    }

    @Override
    public Mono<Set<String>> keySet() {
        return localCache.keySet();
    }

    @Override
    public Flux<String> keys(String pattern, int limit) {
        return localCache.keys(pattern, limit);
    }

    @Override
    public Mono<Boolean> evictMap(String key) {
        return localCache.evictMap(key)
                .doOnSuccess(evicted -> HybridSync.upstream(ruleEvaluatorUseCase, key,
                        () -> centralizedCache.evictMap(key)));
    }

    @Override
    public Mono<Boolean> evictMap(String key, String field) {
        return localCache.evictMap(key, field)
                .doOnSuccess(evicted -> HybridSync.upstream(ruleEvaluatorUseCase, key,
                        () -> centralizedCache.evictMap(key, field)));
    }

}
