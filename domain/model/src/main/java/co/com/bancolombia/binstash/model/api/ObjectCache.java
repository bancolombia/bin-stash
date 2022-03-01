package co.com.bancolombia.binstash.model.api;

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

}

