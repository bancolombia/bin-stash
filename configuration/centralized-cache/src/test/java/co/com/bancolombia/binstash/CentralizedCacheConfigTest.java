package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.adapter.redis.RedisProperties;
import co.com.bancolombia.binstash.config.CentralizedCacheConfig;
import org.junit.jupiter.api.*;
import redis.embedded.RedisServer;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CentralizedCacheConfigTest {

    private static RedisServer redisServer;
    private CentralizedCacheConfig config;
    private RedisProperties redisProperties;

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
        config = new CentralizedCacheConfig();
        redisProperties = new RedisProperties();
        redisProperties.setHost("localhost");
        redisProperties.setPort(16379);
    }

    @Test
    @DisplayName("Create redis stash")
    void createStash() {
        assertNotNull(config.redisStash(redisProperties));
    }

    @Test
    @DisplayName("Create factory")
    void createFactory() {
        assertNotNull(config.newFactory(config.redisStash(redisProperties), new ObjectMapper()));
    }
}
