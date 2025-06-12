package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.MapCache;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;

public class DoubleTierMapCacheUseCase implements MapCache {

    private static final Scheduler elastic_scheduler = Schedulers.boundedElastic();
    private final MapCache localCache;
    private final MapCache centralizedCache;
    private final RuleEvaluatorUseCase ruleEvaluatorUseCase;

    public DoubleTierMapCacheUseCase(MapCache localCache,
                                     MapCache centralizedCache,
                                     RuleEvaluatorUseCase ruleEvaluatorUseCase) {
        this.localCache = localCache;
        this.centralizedCache = centralizedCache;
        this.ruleEvaluatorUseCase = ruleEvaluatorUseCase;
    }

    @Override
    public Mono<Map<String, String>> saveMap(String key, Map<String, String> value) {
        return saveMap(key, value, -1);
    }

    @Override
    public Mono<Map<String, String>> saveMap(String key, Map<String, String> value, int ttl) {
        return localCache.saveMap(key, value, ttl)
                .doAfterTerminate(() ->
                        Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                                .subscribeOn(elastic_scheduler)
                                .filter(shouldSync -> shouldSync)
                                .flatMap(shouldSync -> centralizedCache.existsMap(key))
                                .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                                .flatMap(exists -> centralizedCache.saveMap(key, value, ttl))
                                .subscribe()
                );
    }

    @Override
    public Mono<String> saveMap(String key, String field, String value) {
        return saveMap(key, field, value, -1);
    }

    @Override
    public Mono<String> saveMap(String key, String field, String value, int ttl) {
        return localCache.saveMap(key, field, value, ttl)
                .doAfterTerminate(() ->
                        Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                                .subscribeOn(elastic_scheduler)
                                .filter(shouldSync -> shouldSync)
                                .flatMap(shouldSync -> centralizedCache.existsMap(key, field))
                                .filter(elementExistsInDistCache -> !elementExistsInDistCache)
                                .flatMap(exists -> centralizedCache.saveMap(key, field, value, ttl))
                                .subscribe()
                );
    }

    @Override
    public Mono<String> getMap(String key, String field) {
        return localCache.getMap(key, field)
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(ruleEvaluatorUseCase.evalForUpstreamSync(key))
                    .filter(shouldFetchFromDist -> shouldFetchFromDist)
                    .flatMap(shouldFetch -> this.centralizedCache.getMap(key, field)
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
                    .flatMap(shouldFetch -> this.centralizedCache.getMap(key)
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
    public Mono<Set<String>> keySet() {
        return localCache.keySet();
    }

    @Override
    public Flux<String> keys(String pattern, int limit) {
        return localCache.keys(pattern, limit);
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
