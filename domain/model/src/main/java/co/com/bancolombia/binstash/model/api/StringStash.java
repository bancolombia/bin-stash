package co.com.bancolombia.binstash.model.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Repo for storing key/value <pre>String</pre> data.
 */
public interface StringStash {

    /**
     * Saves a key-value in a repository
     * @param key key to store related value
     * @param value value to be stored
     * @param ttl time the key should live in the stash
     * @return the same value stored.
     */
    Mono<String> save(String key, String value, int ttl);

    /**
     * Saves a key-value in a repository
     * @param key key to store related value
     * @param value value to be stored
     * @return the same value stored.
     */
    Mono<String> save(String key, String value);

    /**
     * Gets a value from the store.
     * @param key the key to be obtainer
     * @return the string value stored under key
     */
    Mono<String> get(String key);

    /**
     * Gets a set of all keys currently stored
     * @return Set o f keys
     */
    Mono<Set<String>> keySet();

    /**
     * Gets a set of keys currently stored that match a given a pattern.
     * @param pattern pattern to match keys against
     * @param limit maximum number of keys to return
     * @return Set o f keys
     */
    Flux<String> keys(String pattern, int limit);

    /**
     * Checks if a given key exists in the repository
     * @param key the key to be checked.
     * @return true if the key exists, false otherwise.
     */
    Mono<Boolean> exists(String key);

    /**
     * Remove the specified key, and its value, from the repo, if such key exists.
     * @param key the key to be evicted.
     * @return true if the key and corresponding value were evicted.
     */
    Mono<Boolean> evict(String key);

    /**
     * Prune whole repository, evicting all keys and its associated values.
     * @return true if the process completed successfully, false otherwise.
     */
    Mono<Boolean> evictAll();

}

