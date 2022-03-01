package co.com.bancolombia.binstash.model.api;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * Repo for storing hash collections of data.
 */
public interface HashStash {

    /**
     * Saves a Map value under key
     * @param key   key value to index map
     * @param value map to store in cache
     * @return inserted map.
     */
    Mono<Map<String, String>> hSave(String key, Map<String, String> value);

    /**
     * Saves a Map value under key,  indicating a Time to live for the data in the cache
     * @param key   key value to index map
     * @param value map to store in cache
     * @param ttl time to live in seconds
     * @return inserted map.
     */
    Mono<Map<String, String>> hSave(String key, Map<String, String> value, int ttl);

    /**
     * Adds/Updates fileds in map.
     *
     * @param key   key value to index map
     * @param field field to update/add into map
     * @param value value to set field to
     * @return field value updated/added in map.
     */
    Mono<String> hSave(String key, String field, String value);

    /**
     * Adds/Updates fileds in map, possibly indicating a ttl for the key in the cache.
     *
     * @param key   key value to index map
     * @param field field to update/add into map
     * @param value value to set field to
     * @param ttl time to live in seconds
     * @return field value updated/added in map.
     */
    Mono<String> hSave(String key, String field, String value, int ttl);

    /**
     * Gets field value from map
     *
     * @param key   key value used to index map.
     * @param field field to get from map
     * @return field value stored in map if exists, Empty Mono otherwise.
     */
    Mono<String> hGet(String key, String field);

    /**
     * Retrieves map stored under 'key'
     * @param key key value used to index map.
     * @return stored map if stored under 'key', Empty Mono otherwise.
     */
    Mono<Map<String, String>> hGetAll(String key);

    /**
     * Gets a set of all keys currently stored
     * @return Set o f keys
     */
    Mono<Set<String>> keySet();

    /**
     * Deletes field from map.
     * @param key key value used to index map.
     * @param field field name used to store value in map.
     * @return true if value is deleted, false otherwise.
     */
    Mono<Boolean> hDelete(String key, String field);

    /**
     * Deletes map.
     * @param key key value used to index map.
     * @return true if map is deleted, false otherwise.
     */
    Mono<Boolean> hDelete(String key);

}

