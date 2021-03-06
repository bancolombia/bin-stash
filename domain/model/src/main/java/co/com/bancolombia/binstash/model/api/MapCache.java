package co.com.bancolombia.binstash.model.api;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * Cache for storing &lt;String, Map&gt; data.
 */
public interface MapCache {

    Mono<Map<String, String>> saveMap(String key, Map<String, String> value);

    Mono<String> saveMap(String key, String field, String value);

    Mono<String> getMap(String key, String field);

    Mono<Map<String, String>> getMap(String key);

    Mono<Boolean> existsMap(String key);

    Mono<Boolean> existsMap(String key, String field);

    /**
     * obtains a Set with all keys stored in cache
     * @return a Mono containing a Set of strings mapping each key that exists in cache.
     */
    Mono<Set<String>> keySet();

    Mono<Boolean> evictMap(String key);

    Mono<Boolean> evictMap(String key, String field);

}

