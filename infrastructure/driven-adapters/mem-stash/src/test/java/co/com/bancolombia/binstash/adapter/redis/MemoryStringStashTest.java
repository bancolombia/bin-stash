package co.com.bancolombia.binstash.adapter.redis;

import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryStringStashTest {

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
    @DisplayName("Should save element whit ttl")
    void testSaveWhitTtl() {
        StepVerifier.create(stash.save(TEST_KEY, TEST_VALUE,1))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
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

    @Test
    @DisplayName("Should allow get all keys")
    void testGetKeys() {
        Mono<Set<String>> op = stash.save(TEST_KEY, TEST_VALUE)
                        .then(stash.keySet());

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(Set.of(TEST_KEY))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should allow get keys given a pattern")
    void testGetKeysPattern() {
        stash.save(TEST_KEY, TEST_VALUE).subscribe();
        Flux<String> op = stash.keys("*", 1);

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(TEST_KEY)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should not get keys given a pattern")
    void testGetKeysPatternMiss() {
        stash.save(TEST_KEY, TEST_VALUE).subscribe();
        Flux<String> op = stash.keys("foo*", 1);

        StepVerifier.create(op)
                .expectSubscription()
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should save element with indexKey and retrieve it")
    void testSSaveAndSetGetAll() {
        String indexKey = "group1";
        StepVerifier.create(stash.setSave(indexKey, TEST_KEY, TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.setGetAll(indexKey))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();
    }


    @Test
    @DisplayName("Should save element with indexKey and ttl, then expire")
    void testSetSaveWithTtlExpire() throws Exception {
        String indexKey = "group2";
        Field field = stash.getClass().getDeclaredField("indexKeyMap");
        field.setAccessible(true);
        ConcurrentHashMap<String, Set<String>> indexKeyMap = (ConcurrentHashMap<String, Set<String>>) field.get(stash);

        StepVerifier.create(stash.setSave(indexKey, TEST_KEY, TEST_VALUE, 1))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();

        assertFalse(indexKeyMap.get(indexKey).isEmpty());

        StepVerifier.create(stash.setGetAll(indexKey).delaySubscription(Duration.ofSeconds(2)))
                .expectSubscription()
                .expectComplete()
                .verify();

        assertTrue(Objects.isNull(indexKeyMap.get(indexKey)));

    }

    @Test
    @DisplayName("Should not save with null indexKey")
    void testSetSaveWithNullIndexKey() {
        StepVerifier.create(stash.setSave(null, TEST_KEY, TEST_VALUE))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should remove element from indexKey")
    void testSetRemove() {
        String indexKey = "group3";

        StepVerifier.create(stash.setSave(indexKey, TEST_KEY, TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.setRemove(indexKey, TEST_KEY))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.setGetAll(indexKey))
                .expectSubscription()
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle setRemove with null key")
    void testSetRemoveWithNullKey() {
        StepVerifier.create(stash.setRemove("group4", null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should handle setGetAll with null indexKey")
    void testSetGetAllWithNullIndexKey() {
        StepVerifier.create(stash.setGetAll(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }
}
