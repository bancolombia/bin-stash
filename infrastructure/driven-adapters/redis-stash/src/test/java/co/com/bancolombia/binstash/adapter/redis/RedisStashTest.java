package co.com.bancolombia.binstash.adapter.redis;

import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log
class RedisStashTest {

    private static final String TEST_VALUE = "Hello World";
    private static RedisServer redisServer;
    private RedisProperties properties;
    private RedisStash stash;
    private Map<String, String> demoMap;

    @BeforeAll
    static void prepare() throws IOException {
        redisServer = new RedisServer(16379);
        redisServer.start();
    }

    @AfterAll
    static void clean() throws IOException {
        redisServer.stop();
    }

    @BeforeEach
    void before() {
        demoMap = new HashMap<>();
        demoMap.put("name", "Peter");
        demoMap.put("lastName", "Parker");

        properties = new RedisProperties();
        properties.setExpireTime(1);
        properties.setHost("127.0.0.1");
        properties.setPort(16379);

        this.stash = RedisStashFactory.redisStash(properties);

        this.stash.evictAll();
    }

    @AfterEach
    void after() {
        this.stash.evictAll().subscribe();
    }

//    @Test
    @DisplayName("Should create instance with defaults")
    void testCreateWithDefaults() {
        assertNotNull(RedisStashFactory.redisStash(properties));
    }

    @Test
    @DisplayName("Should create instance with all props setted")
    void testCreateManual() {
        assertNotNull(RedisStashFactory.redisStash(properties));
    }

    @Test
    @DisplayName("Should create connection to a master-replica cluster")
    void testCreateMasterReplicaConnection() {
        RedisProperties mrProperties = new RedisProperties();
        mrProperties.setExpireTime(1);
        mrProperties.setHost("localhost");
        mrProperties.setHostReplicas("localhost");
        mrProperties.setPort(16379);
        assertNotNull(RedisStashFactory.redisStash(mrProperties));
    }


    @Test
    @DisplayName("Should save element")
    void testPut() {
        StepVerifier.create(stash.save("key1", TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle save with null key")
    void testPutNullKey() {
        StepVerifier.create(stash.save(null, TEST_VALUE))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
        StepVerifier.create(stash.save("key1", null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should save then get element")
    void testPutGet() {
        Mono<String> op = stash.save("key2", TEST_VALUE)
                .then(stash.get("key2"));

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.get(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should verify key exists")
    void testExists() {
        Mono<Boolean> op = stash.save("key2", TEST_VALUE)
                .then(stash.exists("key2"));

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.exists(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should get keyset")
    void testKeySet() {

        Mono<Set<String>> op = stash.save("key2", TEST_VALUE)
                .then(stash.keySet());

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(Set.of("key2"))
                .expectComplete()
                .verify();

    }

    @Test
    @DisplayName("Should save, evict, then try to get element")
    void testPutEvictGet() {
        Mono<String> op = stash.save("key4", TEST_VALUE)
                .then(stash.evict("key4"))
                .then(stash.get("key4"));

        StepVerifier.create(op)
                .expectSubscription()
                .expectComplete()
                .verify();

        StepVerifier.create(stash.evict(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should save keys, then evict all")
    void testPutEvictAll() {
        Mono<String> op = stash.save("key5", TEST_VALUE)
                .then(stash.save("key5b", TEST_VALUE))
                .then(stash.evictAll())
                .then(stash.get("key5"));

        StepVerifier.create(op)
                .expectSubscription()
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should save map")
    void testPutMap() {

        StepVerifier.create(stash.hSave("keyMap", demoMap))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hSave("keyMap", null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();

    }

    @Test
    @DisplayName("Should save field in map")
    void testPutFieldMap() {
        StepVerifier.create(stash.hSave("keyMap", "location", "NJ"))
                .expectSubscription()
                .expectNext("NJ")
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hSave("keyMap", null, ""))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();

    }

    @Test
    @DisplayName("Should get field from map")
    void testGetFieldMap() {
        Mono<String> op = stash.hSave("keyMap", demoMap)
                .then(stash.hGet("keyMap", "name"));

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext("Peter")
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hGet(null, "name"))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should get map")
    void testGetMap() {
        Mono<Map<String, String>> op = stash.hSave("keyMap", demoMap)
                .then(stash.hGetAll("keyMap"));

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(demoMap)
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
        Mono<String> op = stash.hSave("keyMap", demoMap)
                .then(stash.hDelete("keyMap"))
                .then(stash.hGet("keyMap", "name"));

        StepVerifier.create(op)
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
        Mono<Boolean> op = stash.hSave("keyMap", demoMap)
                .then(stash.hDelete("keyMap", "name"));

        StepVerifier.create(op)
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.hDelete(null, "name"))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }
}
