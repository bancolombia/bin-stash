package co.com.bancolombia.binstash.model.api;

import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Repo for storing key/value string data.
 */
public interface StringStash {

    /**
     * Saves a key-value in a repository
     * @param key key to store related value
     * @param value value to be stored
     * @return the same value stored.
     */
    Mono<String> save(String key, String value);

    /**
     * Gets a value from the store.
     * @param key
     * @return
     */
    Mono<String> get(String key);

    Mono<Set<String>> keySet();

    Mono<Boolean> exists(String key);

    Mono<Boolean> evict(String key);

    Mono<Boolean> evictAll();

}

