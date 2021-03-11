package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.StringStash;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

import java.util.Set;

@Log
@RequiredArgsConstructor
public class SingleTierObjectCacheUseCase<T> implements ObjectCache<T> {

    private final StringStash cache;
    private final SerializatorHelper<T> serializatorHelper;

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
    public Mono<Boolean> evict(String key) {
        return cache.evict(key);
    }

    @Override
    public Mono<Boolean> evictAll() {
        return cache.evictAll();
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
