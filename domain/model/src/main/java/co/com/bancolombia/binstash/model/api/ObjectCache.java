package co.com.bancolombia.binstash.model.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * API for storing Objects in cache. Implementor will use <pre>Stash</pre> as a repository, and Objects will be
 * serialized into <pre>String</pre> before invoking the appropriate <pre>Stash</pre> save operations, and deserialized
 * back into the corresponding Type on get operations.
 */
public interface ObjectCache<T> {

    /**
     * Save value to cache
     * @param key key to index value
     * @param value value to store
     * @return value stored
     */
    Mono<T> save(String key, T value);

    /**
     * Save value to cache, alternatively specifying a TTL for the key
     * @param key key to index value
     * @param value value to store
     * @param ttl time key should live in cache
     * @return value stored
     */
    Mono<T> save(String key, T value, int ttl);

    /**
     * Gets an element from cache
     * @param key key to which value was stored
     * @param clazz The class type of object stored for deserialization purposes
     * @return value stored under key or empty if no such key exists in cache.
     */
    Mono<T> get(String key, Class<T> clazz);

    /**
     * Gets an element from cache
     * @param key key to which value was stored
     * @param ref The type reference to process deserialization
     * @return value stored under key or empty if no such key exists in cache.
     */
    Mono<T> get(String key, Object ref);

    /**
     * Asserts if a key is stored in cache
     * @param key the key to verify
     * @return a Mono containing a boolean result. True if key exists in cache, false otherwhise.
     */
    Mono<Boolean> exists(String key);

    /**
     * obtains a Set with all keys stored in cache
     * @return a Mono containing a Set of strings mapping each key that exists in cache.
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
     * Evicts a key-value stored in cache
     * @param key the key to evict
     * @return a Mono containing a boolean result. True if key-value is evicted from cache, false otherwhise.
     */
    Mono<Boolean> evict(String key);

    /**
     * Evicts all keys in cache.
     * @return a Mono containing a boolean result. True if all keys were evicted from cache, false otherwhise.
     */
    Mono<Boolean> evictAll();

    /**
     * Saves a value to a set structure in cache with a specified TTL.
     * This method stores the value as a member of a set identified by the indexKey,
     * with an individual key for the value and a time-to-live for the set.
     *
     * @param indexKey the key that identifies the set in cache
     * @param key the individual key for the value within the set
     * @param value the value to store in the set
     * @param ttl time the set should live in cache (in seconds)
     * @return a Mono containing the value stored
     */
    Mono<T> setSave(String indexKey, String key, T value, int ttl);

    /**
     * Saves a value to a set structure in cache without specifying a TTL.
     * This method stores the value as a member of a set identified by the indexKey,
     * with an individual key for the value. The set will persist according to
     * the cache's default behavior.
     *
     * @param indexKey the key that identifies the set in cache
     * @param key the individual key for the value within the set
     * @param value the value to store in the set
     * @return a Mono containing the value stored
     */
    Mono<T> setSave(String indexKey, String key, T value);

    /**
     * Retrieves all values from a set structure in cache.
     * This method fetches all members of a set identified by the indexKey and
     * deserializes them to the specified class type.
     *
     * <p><strong>Performance Warning:</strong> This method has O(N) time complexity, where N is the number
     * of members in the set. It iterates through every member and fetches its value, resulting in multiple
     * round-trips to Redis (SMEMBERS, then GET or GET + SREM for each key). For large sets, this can cause
     * high latency and increased load on Redis due to many sequential calls.</p>
     *
     * @param indexKey the key that identifies the set in cache
     * @param clazz the class type of objects stored for deserialization purposes
     * @return a Flux emitting all values stored in the set, or empty if the set doesn't exist
     */
    Flux<T> setGetAll(String indexKey, Class<T> clazz);

    /**
     * Removes a specific value from a set structure in cache.
     * This method removes the member identified by the key from the set
     * identified by the indexKey.
     *
     * @param indexKey the key that identifies the set in cache
     * @param key the individual key of the value to remove from the set
     * @return a Mono containing a boolean result. True if the value was removed, false otherwise
     */
    Mono<Boolean> setRemove(String indexKey, String key);

}

