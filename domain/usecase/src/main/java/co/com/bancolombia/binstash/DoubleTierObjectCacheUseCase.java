package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.ObjectCache;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@RequiredArgsConstructor
public class DoubleTierObjectCacheUseCase<T> implements ObjectCache<T> {

    private final ObjectCache<T> localCache;
    private final ObjectCache<T> centralizedCache;
    private final RuleEvaluatorUseCase ruleEvaluatorUseCase;

    @Override
    public Mono<T> save(String key, T value, int ttl) {
        return localCache.save(key, value, ttl)
                .doOnSuccess(saved -> HybridSync.upstream(ruleEvaluatorUseCase, key, () -> centralizedCache.exists(key)
                        .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                        .flatMap(absent -> centralizedCache.save(key, value, ttl))));
    }

    @Override
    public Mono<T> save(String key, T value) {
        return localCache.save(key, value)
                .doOnSuccess(saved -> HybridSync.upstream(ruleEvaluatorUseCase, key, () -> centralizedCache.exists(key)
                        .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                        .flatMap(absent -> centralizedCache.save(key, value))));
    }

    @Override
    public Mono<T> get(String key, Class<T> clazz) {
        return localCache.get(key, clazz)
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .filter(shouldFetchFromDist -> shouldFetchFromDist)
                    .flatMap(shouldFetch -> this.searchCentralized(key, clazz))
            ));
    }

    @Override
    public Mono<T> get(String key, Object ref) {
        return localCache.get(key, ref)
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .filter(shouldFetchFromDist -> shouldFetchFromDist)
                    .flatMap(shouldFetch -> this.searchCentralized(key, ref))
            ));
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return localCache.exists(key);
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
    public Mono<Boolean> evict(String key) {
        return localCache.evict(key)
                .doOnSuccess(evicted -> HybridSync.upstream(ruleEvaluatorUseCase, key,
                        () -> centralizedCache.evict(key)));
    }

    @Override
    public Mono<Boolean> evictAll() {
        // TODO: should sync evictAll event to centralized cache?
        return localCache.evictAll();
    }

    @Override
    public Mono<T> setSave(String indexKey, String key, T value, int ttl) {
        return localCache.setSave(indexKey, key, value, ttl)
                .doOnSuccess(saved -> HybridSync.upstream(ruleEvaluatorUseCase, indexKey,
                        () -> centralizedCache.exists(indexKey)
                                .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                                .flatMap(absent -> centralizedCache.setSave(indexKey, key, value, ttl))));
    }

    @Override
    public Mono<T> setSave(String indexKey, String key, T value) {
        return localCache.setSave(indexKey, key, value)
                .doOnSuccess(saved -> HybridSync.upstream(ruleEvaluatorUseCase, indexKey,
                        () -> centralizedCache.exists(indexKey)
                                .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                                .flatMap(absent -> centralizedCache.setSave(indexKey, key, value))));
    }

    @Override
    public Flux<T> setGetAll(String indexKey, Class<T> clazz) {
        return localCache.setGetAll(indexKey, clazz)
                .switchIfEmpty(Flux.defer(() ->
                        Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(indexKey))
                                .filter(shouldFetchFromDist -> shouldFetchFromDist)
                                .flatMapMany(shouldFetch -> this.searchCentralizedSet(indexKey, clazz))
                ));
    }

    @Override
    public Mono<Boolean> setRemove(String indexKey, String key) {
        return localCache.setRemove(indexKey, key)
                .doOnSuccess(removed -> HybridSync.upstream(ruleEvaluatorUseCase, indexKey,
                        () -> centralizedCache.setRemove(indexKey, key)));
    }

    private Mono<T> searchCentralized(String key, Class<T> clazz) {
        return this.centralizedCache.get(key, clazz)
                .doOnNext(next -> HybridSync.downstream(ruleEvaluatorUseCase, key,
                        () -> this.localCache.save(key, next)));
    }

    private Mono<T> searchCentralized(String key, Object ref) {
        return this.centralizedCache.get(key, ref)
                .doOnNext(next -> HybridSync.downstream(ruleEvaluatorUseCase, key,
                        () -> this.localCache.save(key, next)));
    }

    // Not synced downstream: the centralized set only returns values, not member keys, so it can't be rebuilt locally.
    private Flux<T> searchCentralizedSet(String indexKey, Class<T> clazz) {
        return this.centralizedCache.setGetAll(indexKey, clazz);
    }

}
