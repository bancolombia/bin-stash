package co.com.bancolombia.binstash.adapter.redis;

import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MemoryStashTest {

    private static final String TEST_KEY = "key1";
    private static final String TEST_VALUE = "Hello World";

    private MemoryStash stash;
    private Map<String, String> demoMap;

    @BeforeEach
    void prepare() {
        demoMap = new HashMap<>();
        demoMap.put("name", "Peter");
        demoMap.put("lastName", "Parker");

        stash = new MemoryStash.Builder()
                .expireAfter(1)
                .maxSize(10)
                .build();
    }

    @AfterEach
    void clean() {
        stash.evictAll();
        stash = null;
    }

    @Test
    @DisplayName("Should create instance")
    void testCreate() {
        MemoryStash stash2 = new MemoryStash.Builder()
                .expireAfter(2)
                .maxSize(10)
                .build();
        assertNotNull(stash2);
    }

    @Test
    @DisplayName("Should create instance with defaults")
    void testCreateWithDefaults() {
        MemoryStash stash2 = new MemoryStash.Builder()
                .build();
        assertNotNull(stash2);
    }

    @Test
    @DisplayName("Should save element")
    void testSave() {
        StepVerifier.create(stash.save(TEST_KEY, TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should save hash")
    void testSaveHash() {

        StepVerifier.create(stash.hSave("h1", demoMap))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hSave(null, demoMap))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should save hash element")
    void testSaveHashElement() {
        Mono<String> saveMono = stash.hSave("h2", demoMap)
                .then(stash.hSave("h2", "location", "NJ"));

        StepVerifier.create(saveMono)
                .expectSubscription()
                .expectNext("NJ")
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hSave(null, "location", "NJ"))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should not save with null key")
    void testSaveWithNullKey() {
        StepVerifier.create(stash.save(null, TEST_VALUE))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should save then get element")
    void testSaveGet() {
        Mono<String> op = stash.save(TEST_KEY, TEST_VALUE)
                .then(stash.get(TEST_KEY));

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should get hash element")
    void testGetHashElement() {
        Map<String, String> values = new HashMap<>();
        values.put("email", "pparker@avengers.com");
        values.put("location", "Ny");

        Mono<String> saveMono = stash.hSave("h3", values)
                .then(stash.hGet("h3", "email"));

        StepVerifier.create(saveMono)
                .expectSubscription()
                .expectNext("pparker@avengers.com")
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hGet("h3", null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should get hash map")
    void testGetHashMap() {
        Map<String, String> values = new HashMap<>();
        values.put("email", "pparker@avengers.com");
        values.put("location", "NJ");

        Mono<Map<String, String>> saveMono = stash.hSave("h4", values)
                .then(stash.hGetAll("h4"));

        StepVerifier.create(saveMono)
                .expectSubscription()
                .expectNext(values)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hGetAll(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should delete map")
    void testDeleteMap() {
        Mono<Map<String, String>> saveMono = stash.hSave("h5", demoMap)
                .then(stash.hDelete("h5"))
                .then(stash.hGetAll("h5"));

        StepVerifier.create(saveMono)
                .expectSubscription()
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hDelete(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should delete field from map")
    void testDeleteFieldMap() {
        Mono<Map<String, String>> saveMono = stash.hSave("h6", demoMap)
                .then(stash.hDelete("h6", "name"))
                .then(stash.hGetAll("h6"));

        StepVerifier.create(saveMono)
                .expectSubscription()
                .expectNextMatches(data -> {
                    assert data.containsKey("lastName");
                    assert !data.containsKey("name");
                    return true;
                })
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hDelete(null, "xx"))
                .expectSubscription()
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should get empty on unexistent key")
    void testGetEmpty() {
        StepVerifier.create(stash.get("unexistent-key"))
                .expectSubscription()
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle get with null key")
    void testGetNullKey() {
        StepVerifier.create(stash.get(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should get keys")
    void testKeySet() {

        Mono<Set<String>> keysMono = stash.hSave("h6", demoMap)
                .then(stash.keySet());

        List<String> expectedKeys = demoMap.keySet().stream()
                .map(key -> "h6-"+key)
                .collect(Collectors.toList());

        StepVerifier.create(keysMono)
                .expectSubscription()
                .expectNext(new HashSet<>(expectedKeys))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should save, expire, then try to get element")
    void testPutExpireGet() {
        Mono<String> op = stash.save(TEST_KEY, TEST_VALUE)
                .delayElement(Duration.ofSeconds(2))
                .then(stash.get(TEST_KEY));

        StepVerifier.create(op)
                .expectSubscription()
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should save, evict, then try to get element")
    void testPutEvictGet() {
        Mono<String> op = stash.save(TEST_KEY, TEST_VALUE)
                .then(stash.evict(TEST_KEY))
                .then(stash.get(TEST_KEY));

        StepVerifier.create(op)
                .expectSubscription()
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should test if key exists")
    void testExists() {
        Mono<Boolean> op = stash.save(TEST_KEY, TEST_VALUE)
                .then(stash.exists(TEST_KEY));

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.exists("unexistent-key"))
                .expectSubscription()
                .expectNext(false)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.exists(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }


    @Test
    @DisplayName("Should handle null key on evit")
    void testEvictWithNullKey() {
        StepVerifier.create(stash.evict(null))
                .expectSubscription()
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should save keys, then evict all")
    void testPutEvictAll() {
        Mono<String> op = stash.save(TEST_KEY, TEST_VALUE)
                .then(stash.save(TEST_KEY+"b", TEST_VALUE))
                .then(stash.evictAll())
                .then(stash.get(TEST_KEY));

        StepVerifier.create(op)
                .expectSubscription()
                .expectComplete()
                .verify();
    }
}
