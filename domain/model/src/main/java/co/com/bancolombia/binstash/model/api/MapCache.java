package co.com.bancolombia.binstash.model.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * Cache API for storing multiple <pre>Map&lt;String, String&gt;</pre> data in cache. Implementor will use the <pre>HashStash</pre>
 * as repository.
 */
public interface MapCache {

    /**
     * Stores the argument Map into the cache under the provided key.
     * @param key the key under which the map should be stored into.
     * @param value the map to store
     * @return the original map.
     */
    Mono<Map<String, String>> saveMap(String key, Map<String, String> value);

    /**
     * Stores the argument Map into the cache under the provided key, and setting the expiration
     * of the key
     * @param key the key under which the map should be stored into.
     * @param value the map to store
     * @param ttl the time to live of the key in the cache
     * @return the original map.
     */
    Mono<Map<String, String>> saveMap(String key, Map<String, String> value, int ttl);

    /**
     * Creates a new map, if it doesn't previously exist in the cache with the provided <pre>key</pre>, and stores
     * the field and value into such map. If the map exists, then add/updates the field-value.
     * @param key the key under which the map should be stored into.
     * @param field the field to store
     * @param value the value to store
     * @return the original map.
     */
    Mono<String> saveMap(String key, String field, String value);

    /**
     * Creates a new map, if it doesn't previously exist in the cache with the provided <pre>key</pre>, and stores
     * the field and value into such map. If the map exists, then add/updates the field-value.
     * @param key the key under which the map should be stored into.
     * @param field the field to store
     * @param value the value to store
     * @param ttl the time to live of the key in the cache
     * @return the original map.
     */
    Mono<String> saveMap(String key, String field, String value, int ttl);

    /**
     * Fetches a value stored in a map in the cache.
     * @param key the key under which the map exists in the cache.
     * @param field the name of the field in the map.
     * @return the string value associated to the field name.
     */
    Mono<String> getMap(String key, String field);

    /**
     * Fetches the whole map stored under a key in the cache.
     * @param key the key under which the map exists in the cache.
     * @return the Map object.
     */
    Mono<Map<String, String>> getMap(String key);

    /**
     * Checks whether a map is stored under a given key in the cache.
     * @param key the key to check if exists in the cache.
     * @return true if there is a map stored in the cache with such key, false otherwise.
     */
    Mono<Boolean> existsMap(String key);

    /**
     * Checks whether field exists within a map stored under a given key in the cache.
     * @param key the key to check if exists as a map in the cache.
     * @param field the field value to check within the map.
     * @return true if there is a map stored in the cache with such key, and a field with a given name in the map,
     * or false otherwise.
     */
    Mono<Boolean> existsMap(String key, String field);

    /**
     * Retrieves all keys existing in the cache. Note this operation will return ALL keys existing in the underlying
     * cache whether those keys represent maps or single key-values.
     * @return a set of all keys.
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
     * Removes a map identified by key, effectively removing all field-value pairs.
     * @param key the key under which the map is stored in the cache.
     * @return true if the map was evicted, false otherwise.
     */
    Mono<Boolean> evictMap(String key);

    /**
     * Removes a field from a map identified by key, effectively removing a single field-value pair.
     * @param key the key under which the map is stored in the cache.
     * @param field the field under which the value is stored in the map.
     * @return true if the field-value was removed from map, false otherwise.
     */
    Mono<Boolean> evictMap(String key, String field);

}

