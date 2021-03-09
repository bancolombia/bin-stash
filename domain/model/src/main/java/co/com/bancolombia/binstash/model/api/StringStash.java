package co.com.bancolombia.binstash.model.api;

import reactor.core.publisher.Mono;

/**
 * Repo for storing key/value string data.
 */
public interface StringStash {

    Mono<String> save(String key, String value);

    Mono<String> get(String key);

    Mono<Boolean> exists(String key);

    Mono<Boolean> evict(String key);

    Mono<Boolean> evictAll();

}

