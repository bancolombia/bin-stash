package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.ObjectCache;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Set;

public class DoubleTierObjectCacheUseCase<T> implements ObjectCache<T> {

    private static final Scheduler elastic_scheduler = Schedulers.boundedElastic();
    private final ObjectCache<T> localCache;
    private final ObjectCache<T> centralizedCache;
    private final RuleEvaluatorUseCase ruleEvaluatorUseCase;

    public DoubleTierObjectCacheUseCase(ObjectCache<T> localCache,
                                        ObjectCache<T> centralizedCache,
                                        RuleEvaluatorUseCase ruleEvaluatorUseCase) {
        this.localCache = localCache;
        this.centralizedCache = centralizedCache;
        this.ruleEvaluatorUseCase = ruleEvaluatorUseCase;
    }

    @Override
    public Mono<T> save(String key, T value) {
        return localCache.save(key, value)
            .doAfterTerminate(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                        .subscribeOn(elastic_scheduler)
                        .filter(shouldSync -> shouldSync)
                        .flatMap(shouldSync -> centralizedCache.exists(key))
                        .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                        .flatMap(exists -> centralizedCache.save(key, value))
                        .subscribe()
            );
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
    public Mono<Boolean> evict(String key) {
        return localCache.evict(key)
            .doAfterTerminate(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .subscribeOn(elastic_scheduler)
                    .filter(shouldSyncUpstream -> shouldSyncUpstream)
                    .flatMap(shouldSync -> centralizedCache.evict(key))
                    .subscribe()
            );
    }

    @Override
    public Mono<Boolean> evictAll() {
        // TODO: should sync evictAll event to centralized cache?
        return localCache.evictAll();
    }

    private Mono<T> searchCentralized(String key, Class<T> clazz) {
        return this.centralizedCache.get(key, clazz)
                .doOnNext(next ->
                        Mono.just(ruleEvaluatorUseCase.evalForDownstreamSync(key))
                                .filter(shouldSyncFromDist -> shouldSyncFromDist)
                                .flatMap(shouldSync -> this.localCache.save(key, next))
                                .subscribe()
                );
    }

    private Mono<T> searchCentralized(String key, Object ref) {
        return this.centralizedCache.get(key, ref)
                .doOnNext(next ->
                        Mono.just(ruleEvaluatorUseCase.evalForDownstreamSync(key))
                                .filter(shouldSyncFromDist -> shouldSyncFromDist)
                                .flatMap(shouldSync -> this.localCache.save(key, next))
                                .subscribe()
                );
    }

}
