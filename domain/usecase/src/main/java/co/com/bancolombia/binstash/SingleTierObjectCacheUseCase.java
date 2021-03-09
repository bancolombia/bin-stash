package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.StringStash;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

@Log
@RequiredArgsConstructor
public class SingleTierObjectCacheUseCase<T> implements ObjectCache<T> {

    private final StringStash cache;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<T> save(String key, T value) {
        return Mono.just(value)
                .map(this::serialize)
                .flatMap(serialized -> cache.save(key, serialized))
                .map(r -> value);
    }

    @Override
    public Mono<T> get(String key, Class<T> clazz) {
        return Mono.just(key)
                .flatMap(cache::get)
                .map(serialized -> this.deserialize(serialized, clazz));
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return Mono.just(key)
                .flatMap(cache::exists);
    }

    @Override
    public Mono<Boolean> evict(String key) {
        return cache.evict(key);
    }

    @Override
    public Mono<Boolean> evictAll() {
        return cache.evictAll();
    }

    private String serialize(T obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.severe(e.getMessage());
            return null;
        }
    }

    private T deserialize(String obj, Class<T> clazz) {
        try {
            return objectMapper.readValue(obj, clazz);
        } catch (JsonProcessingException e) {
            log.severe(e.getMessage());
            return null;
        }
    }
}
