package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.MapCache;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

public class DoubleTierMapCacheUseCase implements MapCache {

    private final MapCache localCache;
    private final MapCache distributedCache;
    private final RuleEvaluatorUseCase ruleEvaluatorUseCase;
    private final static Scheduler elastic_scheduler = Schedulers.boundedElastic();

    public DoubleTierMapCacheUseCase(MapCache localCache,
                                     MapCache distributedCache,
                                     RuleEvaluatorUseCase ruleEvaluatorUseCase) {
        this.localCache = localCache;
        this.distributedCache = distributedCache;
        this.ruleEvaluatorUseCase = ruleEvaluatorUseCase;
    }

    @Override
    public Mono<Map<String, String>> saveMap(String key, Map<String, String> value) {
        return localCache.saveMap(key, value)
                .doAfterTerminate(() ->
                    Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                            .subscribeOn(elastic_scheduler)
                            .filter(shouldSync -> shouldSync)
                            .flatMap(shouldSync -> distributedCache.existsMap(key))
                            .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                            .flatMap(exists -> distributedCache.saveMap(key, value))
                            .subscribe()
                );
    }

    @Override
    public Mono<String> saveMap(String key, String field, String value) {
        return localCache.saveMap(key, field, value)
                .doAfterTerminate(() ->
                        Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                                .subscribeOn(elastic_scheduler)
                                .filter(shouldSync -> shouldSync)
                                .flatMap(shouldSync -> distributedCache.existsMap(key, field))
                                .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                                .flatMap(exists -> distributedCache.saveMap(key, field, value))
                                .subscribe()
                );
    }

    @Override
    public Mono<String> getMap(String key, String field) {
        return localCache.getMap(key, field)
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .filter(shouldFetchFromDist -> shouldFetchFromDist)
                    .flatMap(shouldFetch -> this.distributedCache.getMap(key, field)
                        .doOnNext(next ->
                            Mono.just(ruleEvaluatorUseCase.evalForDownstreamSync(key))
                                .filter(shouldSyncFromDist -> shouldSyncFromDist)
                                .flatMap(shouldSync -> this.localCache.saveMap(key, field, next))
                                .subscribe()
                        ))
            ));
    }

    @Override
    public Mono<Map<String, String>> getMap(String key) {
        return localCache.getMap(key)
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .filter(shouldFetchFromDist -> shouldFetchFromDist)
                    .flatMap(shouldFetch -> this.distributedCache.getMap(key)
                        .doOnNext(next ->
                            Mono.just(ruleEvaluatorUseCase.evalForDownstreamSync(key))
                                .filter(shouldSyncFromDist -> shouldSyncFromDist)
                                .flatMap(shouldSync -> this.localCache.saveMap(key, next))
                                .subscribe()
                        ))
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
    public Mono<Boolean> evictMap(String key) {
        return localCache.evictMap(key);
    }

    @Override
    public Mono<Boolean> evictMap(String key, String field) {
        return localCache.evictMap(key, field);
    }

}
