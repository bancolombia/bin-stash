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
     * @param limit maximum number of keys to return. If equal or less than 0, no limit is applied.
     *              The limit is a hint, and the actual number of keys returned could be more or less than the limit,
     *              depending on the concrete service implementation or backend service (eg. Redis).
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

    /**
     * Saves a key-value pair in a set identified by indexKey, indicating a Time to live for the data in the cache
     * @param indexKey the identifier of the set collection
     * @param key the key to be stored in the set
     * @param value the value to be stored under the key
     * @param ttl time to live in seconds
     * @return the value stored.
     */
    Mono<String> setSave(String indexKey, String key, String value, int ttl);

    /**
     * Saves a key-value pair in a set identified by indexKey
     * @param indexKey the identifier of the set collection
     * @param key the key to be stored in the set
     * @param value the value to be stored under the key
     * @return the value stored.
     */
    Mono<String> setSave(String indexKey, String key, String value);

    /**
     * Retrieves all values from the set identified by indexKey
     * @param indexKey the identifier of the set collection
     * @return all values stored in the set if exists, Empty Flux otherwise.
     */
    Flux<String> setGetAll(String indexKey);

    /**
     * Removes a key-value pair from the set identified by indexKey
     * @param indexKey the identifier of the set collection
     * @param key the key to be removed from the set
     * @return true if the key-value pair was removed, false otherwise.
     */
    Mono<Boolean> setRemove(String indexKey, String key);

}

