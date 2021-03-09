package co.com.bancolombia.binstash.model.api;

import reactor.core.publisher.Mono;

/**
 * Cache for storing &lt;String, T&gt; data.
 */
public interface ObjectCache<T> {

    Mono<T> save(String key, T value);

    Mono<T> get(String key, Class<T> clazz);

    Mono<Boolean> exists(String key);

    Mono<Boolean> evict(String key);

    Mono<Boolean> evictAll();

}

