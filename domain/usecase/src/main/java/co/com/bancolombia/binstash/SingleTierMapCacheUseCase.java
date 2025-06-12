package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.HashStash;
import co.com.bancolombia.binstash.model.api.MapCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@Log
@RequiredArgsConstructor
public class SingleTierMapCacheUseCase implements MapCache {

    private final HashStash stash;

    @Override
    public Mono<Map<String, String>> saveMap(String key, Map<String, String> value) {
        return stash.hSave(key, value);
    }

    @Override
    public Mono<Map<String, String>> saveMap(String key, Map<String, String> value, int ttl) {
        return stash.hSave(key, value, ttl);
    }

    @Override
    public Mono<String> saveMap(String key, String field, String value) {
        return stash.hSave(key, field, value);
    }

    @Override
    public Mono<String> saveMap(String key, String field, String value, int ttl) {
        return stash.hSave(key, field, value, ttl);
    }

    @Override
    public Mono<String> getMap(String key, String field) {
        return stash.hGet(key, field);
    }

    @Override
    public Mono<Map<String, String>> getMap(String key) {
        return stash.hGetAll(key);
    }

    @Override
    public Mono<Boolean> existsMap(String key) {
        return stash.hGetAll(key).hasElement();
    }

    @Override
    public Mono<Boolean> existsMap(String key, String field) {
        return stash.hGet(key, field).hasElement();
    }

    @Override
    public Mono<Set<String>> keySet() {
        return stash.keySet();
    }

    @Override
    public Flux<String> keys(String pattern, int limit) {
        return stash.keys(pattern, limit);
    }

    @Override
    public Mono<Boolean> evictMap(String key) {
        return stash.hDelete(key);
    }

    @Override
    public Mono<Boolean> evictMap(String key, String name) {
        return stash.hDelete(key, name);
    }

}
