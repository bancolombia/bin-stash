package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.adapter.redis.RedisProperties;
import co.com.bancolombia.binstash.config.HybridCacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import redis.embedded.RedisServer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class HybridCacheConfigTest {

    private static RedisServer redisServer;
    private HybridCacheConfig config;

    private RedisProperties redisProperties;

    @BeforeAll
    static void prepare() throws IOException {
        redisServer = new RedisServer(16380);
        redisServer.start();
    }

    @AfterAll
    static void clean() throws IOException {
        redisServer.stop();
    }

    @BeforeEach
    void before() {
        config = new HybridCacheConfig();
        redisProperties = new RedisProperties();
        redisProperties.setHost("localhost");
        redisProperties.setPort(16380);
    }

    @Test
    @DisplayName("Create object memory stash")
    void createMemStash() {
        assertNotNull(config.memStash(30, 1_000));
    }

    @Test
    @DisplayName("Create object redis stash")
    void createRedisStash() {
        assertNotNull(config.redisStash(redisProperties));
    }

    @Test
    @DisplayName("Create map memory cache")
    void createMapLocalStash() {
        assertNotNull(config.localMapCache(config.memStash(30, 1_000)));
    }

    @Test
    @DisplayName("Create map redis cache")
    void createMapDistrStash() {
        assertNotNull(config.centralizedMapCache(config.redisStash(redisProperties)));
    }

    @Test
    @DisplayName("Create object memory cache")
    void createObjectLocalStash() {
        assertNotNull(config.localObjectCache(config.memStash(30, 1_000), new ObjectMapper()));
    }

    @Test
    @DisplayName("Create object redis cache")
    void createObjectDistrStash() {
        assertNotNull(config.centralizedObjectCache(config.redisStash(redisProperties),
                new ObjectMapper()));
    }

    @Test
    @DisplayName("Create factory")
    void createFactory() {
        assertNotNull(config.hybridCacheFactory(
                config.localObjectCache(config.memStash(30, 1_000), new ObjectMapper()),
                config.centralizedObjectCache(config.redisStash(redisProperties), new ObjectMapper()),
                config.localMapCache(config.memStash(30, 1_000)),
                config.centralizedMapCache(config.redisStash(redisProperties))
                )
        );
    }
}
