package co.com.bancolombia.binstash.adapter.redis;

import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Log
class RedisStashTest {

    private static final String TEST_VALUE = "Hello World";
    private static final String TEST_INDEX_KEY = "setKey";
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
    @DisplayName("Should get keys given a pattern")
    void testKeysSearch() {

        stash.save("key1", TEST_VALUE).subscribe();
        stash.save("key2", TEST_VALUE).subscribe();
        stash.save("key3", TEST_VALUE).subscribe();
        stash.save("key4", TEST_VALUE).subscribe();
        stash.save("key5", TEST_VALUE).subscribe();
        stash.save("key6", TEST_VALUE).subscribe();
        stash.save("key7", TEST_VALUE).subscribe();
        stash.save("key8", TEST_VALUE).subscribe();
        stash.save("key9", TEST_VALUE).subscribe();
        stash.save("key10", TEST_VALUE).subscribe();
        stash.save("key11", TEST_VALUE).subscribe();
        stash.save("key12", TEST_VALUE).subscribe();
        stash.save("key13", TEST_VALUE).subscribe();
        stash.save("key14", TEST_VALUE).subscribe();
        stash.save("key15", TEST_VALUE).subscribe();
        stash.save("foo1", "bar1").subscribe();

        Mono<List<String>> cappedKeys = stash.keys("k*", 5).collectList();
        StepVerifier.create(cappedKeys)
                .expectSubscription()
                .expectNextMatches(ks -> ks.size() < 15)
                .expectComplete()
                .verify();

        Flux<String> unboundedKeys = stash.keys("k*", -1);
        StepVerifier.create(unboundedKeys)
                .expectSubscription()
                .expectNextCount(15)
                .expectComplete()
                .verify();

        Flux<String> noKeys = stash.keys("acme*", -1);
        StepVerifier.create(noKeys)
                .expectSubscription()
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle error when scanning keys")
    void testKeysSearchWithError() {

        RedisStash mockedStash = Mockito.mock(RedisStash.class);
        when(mockedStash.keys(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(Flux.error(new RuntimeException("Scan error")));

        Flux<String> k = mockedStash.keys("k*", 0);

        StepVerifier.create(k)
                .expectErrorMessage("Scan error")
                .verify();
    }

    @Test
    @DisplayName("Should now get keys given a pattern")
    void testKeysSearchEmpty() {

        stash.save("key2", TEST_VALUE).subscribe();

        Flux<String> k = stash.keys("foo*", 10);

        StepVerifier.create(k)
                .expectSubscription()
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

    @Test
    @DisplayName("Should save element in set and retrieve it")
    void testSSaveAndSetGetAll() {
        StepVerifier.create(stash.setSave(TEST_INDEX_KEY, "key1", TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.setGetAll(TEST_INDEX_KEY))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle sSave with null arguments")
    void testSetSaveNullArgs() {
        StepVerifier.create(stash.setSave(null, "key1", TEST_VALUE))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();

        StepVerifier.create(stash.setSave(TEST_INDEX_KEY, null, TEST_VALUE))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();

        StepVerifier.create(stash.setSave(TEST_INDEX_KEY, "key1", null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should remove element from set")
    void testSetRemove() {
        StepVerifier.create(stash.setSave(TEST_INDEX_KEY, "key1", TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.setRemove(TEST_INDEX_KEY, "key1"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle setRemove with null arguments")
    void testSetRemoveNullArgs() {
        StepVerifier.create(stash.setRemove(null, "key1"))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();

        StepVerifier.create(stash.setRemove(TEST_INDEX_KEY, null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should handle setGetAll with null argument")
    void testSetGetAllNullArg() {
        StepVerifier.create(stash.setGetAll(null))
                .expectSubscription()
                .expectErrorMessage("Caching key cannot be null")
                .verify();
    }

    @Test
    @DisplayName("Should remove set reference if item does not exist")
    void testSetGetAllRemovesMissingItem() {
        StepVerifier.create(stash.setSave(TEST_INDEX_KEY, "key1", TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.evict("key1"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.setGetAll(TEST_INDEX_KEY))
                .expectSubscription()
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should return false when removing non-existent key from set")
    void testSetRemoveNonExistentKey() {
        StepVerifier.create(stash.setRemove(TEST_INDEX_KEY, "nonExistentKey"))
                .expectSubscription()
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should return false when removing non-existent field from map")
    void testHDeleteNonExistentField() {
        StepVerifier.create(stash.setSave(TEST_INDEX_KEY, "key1", TEST_VALUE))
                .expectSubscription()
                .expectNext(TEST_VALUE)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.evict("key1"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(stash.setRemove(TEST_INDEX_KEY, "key1"))
                .expectSubscription()
                .expectNext(false)
                .expectComplete()
                .verify();

    }
}
