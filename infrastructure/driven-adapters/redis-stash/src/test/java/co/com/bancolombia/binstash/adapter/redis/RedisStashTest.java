package co.com.bancolombia.binstash.adapter.redis;

import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log
public class RedisStashTest {

    private static final String TEST_VALUE = "Hello World";
    private static RedisServer redisServer;
    private RedisStash stash;
    private Map<String, String> demoMap;

    @BeforeAll
    public static void prepare() throws IOException {
        redisServer = new RedisServer(16379);
        redisServer.start();
    }

    @AfterAll
    public static void clean() {
        redisServer.stop();
    }

    @BeforeEach
    public void before() {
        demoMap = new HashMap<>();
        demoMap.put("name", "Peter");
        demoMap.put("lastName", "Parker");

        this.stash = new RedisStash.Builder()
                .expireAfter(1)
                .host("127.0.0.1")
                .port(16379)
                .build();
    }

    @AfterEach
    public void after() {
        this.stash.evictAll();
        this.stash = null;
    }

//    @Test
    @DisplayName("Should create instance with defaults")
    public void testCreateWithDefaults() {
        assertNotNull(new RedisStash.Builder().build());
    }

    @Test
    @DisplayName("Should create instance with all props setted")
    public void testCreateManual() {
        assertNotNull(new RedisStash.Builder()
                .db(0)
                .expireAfter(1)
                .host("localhost")
                .port(16379)
//                .password("mypwd")
                .build()
        );
    }


    @Test
    @DisplayName("Should save element")
    public void testPut() {
        StepVerifier.create(stash.save("key1", TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle save with null key")
    public void testPutNullKey() {
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
    public void testPutGet() {
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
    public void testExists() {
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

//    @Test
//    @DisplayName("Should save, expire, then try to get element")
//    public void testPutExpireGet() {
//        Mono<String> op = stash.save("key3", TEST_VALUE)
//                .delayElement(Duration.ofSeconds(3))
//                .then(stash.get("key3"));
//
//        StepVerifier.create(op)
//                .expectSubscription()
//                .expectComplete()
//                .verify();
//    }

    @Test
    @DisplayName("Should save, evict, then try to get element")
    public void testPutEvictGet() {
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
    public void testPutEvictAll() {
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
    public void testPutMap() {

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
    public void testPutFieldMap() {
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
    public void testGetFieldMap() {
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
    public void testGetMap() {
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
    public void testDeleteMap() {
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
    public void testDeleteFieldMap() {
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
