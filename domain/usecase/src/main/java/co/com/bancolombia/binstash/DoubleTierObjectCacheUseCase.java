package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.ObjectCache;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class DoubleTierObjectCacheUseCase<T> implements ObjectCache<T> {

    private final ObjectCache<T> localCache;
    private final ObjectCache<T> distributedCache;
    private final RuleEvaluatorUseCase ruleEvaluatorUseCase;
    private final static Scheduler elastic_scheduler = Schedulers.boundedElastic();

    public DoubleTierObjectCacheUseCase(ObjectCache<T> localCache,
                                        ObjectCache<T> distributedCache,
                                        RuleEvaluatorUseCase ruleEvaluatorUseCase) {
        this.localCache = localCache;
        this.distributedCache = distributedCache;
        this.ruleEvaluatorUseCase = ruleEvaluatorUseCase;
    }

    @Override
    public Mono<T> save(String key, T value) {
        return localCache.save(key, value)
            .doAfterTerminate(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                        .subscribeOn(elastic_scheduler)
                        .filter(shouldSync -> shouldSync)
                        .flatMap(shouldSync -> distributedCache.exists(key))
                        .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                        .flatMap(exists -> distributedCache.save(key, value))
                        .subscribe()
            );
    }

    @Override
    public Mono<T> get(String key, Class<T> clazz) {
        return localCache.get(key, clazz)
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .filter(shouldFetchFromDist -> shouldFetchFromDist)
                    .flatMap(shouldFetch -> this.searchDistributed(key, clazz))
            ));
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return localCache.exists(key);
    }

    private Mono<T> searchDistributed(String key, Class<T> clazz) {
        return this.distributedCache.get(key, clazz)
                .doOnNext(next ->
                    Mono.just(ruleEvaluatorUseCase.evalForDownstreamSync(key))
                        .filter(shouldSyncFromDist -> shouldSyncFromDist)
                        .flatMap(shouldSync -> this.localCache.save(key, next))
                        .subscribe()
                    );
    }

    @Override
    public Mono<Boolean> evict(String key) {
        return localCache.evict(key)
            .doAfterTerminate(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .subscribeOn(elastic_scheduler)
                    .filter(shouldSyncUpstream -> shouldSyncUpstream)
                    .flatMap(shouldSync -> distributedCache.evict(key))
                    .subscribe()
            );
    }

    @Override
    public Mono<Boolean> evictAll() {
        // TODO: should sync evictAll event to distributed cache?
        return localCache.evictAll();
    }

}
