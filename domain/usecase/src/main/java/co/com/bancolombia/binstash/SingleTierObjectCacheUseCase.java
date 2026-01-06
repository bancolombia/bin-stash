package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.InvalidValueException;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.StringStash;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;

import java.util.Set;

@Log
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class SingleTierObjectCacheUseCase<T> implements ObjectCache<T> {

    private final StringStash cache;
    private final SerializatorHelper<T> serializatorHelper;

    @Override
    public Mono<T> save(String key, T value) {
        return save(key, value, -1);
    }

    @Override
    public Mono<T> save(String key, T value, int ttl) {
        if (value == null) {
            return Mono.error(new InvalidValueException("Value cannot be null"));
        } else {
            return Mono.just(value)
                    .map(this::serialize)
                    .flatMap(serialized -> cache.save(key, serialized, ttl))
                    .map(r -> value);
        }
    }

    @Override
    public Mono<T> get(String key, Class<T> clazz) {
        return Mono.just(key)
                .flatMap(cache::get)
                .map(serialized -> this.deserialize(serialized, clazz));
    }

    @Override
    public Mono<T> get(String key, Object ref) {
        return Mono.just(key)
                .flatMap(cache::get)
                .map(serialized -> this.deserialize(serialized, (TypeReference<? extends T>) ref));
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return Mono.just(key)
                .flatMap(cache::exists);
    }

    @Override
    public Mono<Set<String>> keySet() {
        return cache.keySet();
    }

    @Override
    public Flux<String> keys(String pattern, int limit) {
        return cache.keys(pattern, limit);
    }

    @Override
    public Mono<Boolean> evict(String key) {
        return cache.evict(key);
    }

    @Override
    public Mono<Boolean> evictAll() {
        return cache.evictAll();
    }

    @Override
    public Mono<T> setSave(String indexKey, String key, T value, int ttl) {
        if (value == null) {
            return Mono.error(new InvalidValueException("Value cannot be null"));
        } else {
            return Mono.just(value)
                    .map(this::serialize)
                    .flatMap(serialized -> cache.setSave(indexKey, key, serialized, ttl))
                    .map(r -> value);
        }
    }

    @Override
    public Mono<T> setSave(String indexKey, String key, T value) {
        return this.setSave(indexKey, key, value, -1);
    }

    @Override
    public Flux<T> setGetAll(String indexKey, Class<T> clazz) {
        return cache.setGetAll(indexKey)
                .map(value -> this.deserialize(value, clazz));
    }

    @Override
    public Mono<Boolean> setRemove(String indexKey, String key) {
        return cache.setRemove(indexKey, key);
    }

    private String serialize(T obj) {
        return serializatorHelper.serialize(obj);
    }

    private T deserialize(String obj, Class<T> clazz) {
        return this.serializatorHelper.deserializeTo(obj, clazz);
    }

    private T deserialize(String obj, TypeReference<? extends T> ref) {
        return this.serializatorHelper.deserializeWith(obj, ref);
    }
}
