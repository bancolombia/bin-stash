package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.InvalidValueException;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.StringStash;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;

import java.util.Set;

/**
 * Object cache over a single {@link StringStash}.
 * <p>
 * A cached value that cannot be deserialized is treated as a cache miss: {@code get} completes empty and
 * {@code setGetAll} skips it. The entry is not evicted, because the stash may be shared with readers that can
 * deserialize it (e.g. another version of the type during a rolling deploy). It is replaced when its TTL expires
 * or by a later save on this cache; in the hybrid cache the upstream sync only writes keys that do not exist yet,
 * so an unreadable centralized entry is replaced only when its TTL expires.
 * A value that cannot be serialized fails {@code save} and {@code setSave} with {@link InvalidValueException}, and
 * a {@code null} target type fails {@code get} and {@code setGetAll} with {@link InvalidValueException}.
 */
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class SingleTierObjectCacheUseCase<T> implements ObjectCache<T> {

    private static final String NULL_TYPE_MSG = "Target type cannot be null";

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
            return serialize(value)
                    .flatMap(serialized -> cache.save(key, serialized, ttl))
                    .map(r -> value);
        }
    }

    @Override
    public Mono<T> get(String key, Class<T> clazz) {
        if (clazz == null) {
            return Mono.error(new InvalidValueException(NULL_TYPE_MSG));
        }
        return Mono.just(key)
                .flatMap(cache::get)
                .mapNotNull(serialized -> this.deserialize(serialized, clazz));
    }

    @Override
    public Mono<T> get(String key, Object ref) {
        if (ref == null) {
            return Mono.error(new InvalidValueException(NULL_TYPE_MSG));
        }
        return Mono.just(key)
                .flatMap(cache::get)
                .mapNotNull(serialized -> this.deserialize(serialized, (TypeReference<? extends T>) ref));
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
            return serialize(value)
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
        if (clazz == null) {
            return Flux.error(new InvalidValueException(NULL_TYPE_MSG));
        }
        return cache.setGetAll(indexKey)
                .mapNotNull(value -> this.deserialize(value, clazz));
    }

    @Override
    public Mono<Boolean> setRemove(String indexKey, String key) {
        return cache.setRemove(indexKey, key);
    }

    private Mono<String> serialize(T obj) {
        return Mono.fromSupplier(() -> serializatorHelper.serialize(obj))
                .switchIfEmpty(Mono.error(() -> new InvalidValueException("Value could not be serialized")));
    }

    private T deserialize(String obj, Class<T> clazz) {
        return this.serializatorHelper.deserializeTo(obj, clazz);
    }

    private T deserialize(String obj, TypeReference<? extends T> ref) {
        return this.serializatorHelper.deserializeWith(obj, ref);
    }
}
