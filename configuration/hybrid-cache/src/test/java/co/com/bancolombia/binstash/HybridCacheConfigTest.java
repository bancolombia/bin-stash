package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.config.HybridCacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import redis.embedded.RedisServer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class HybridCacheConfigTest {

    private static RedisServer redisServer;
    private HybridCacheConfig config;

    @BeforeAll
    static void prepare() throws IOException {
        redisServer = new RedisServer(16380);
        redisServer.start();
    }

    @AfterAll
    static void clean() {
        redisServer.stop();
    }

    @BeforeEach
    void before() {
        config = new HybridCacheConfig();
        ReflectionTestUtils.setField(config, "localExpireTime", 1);
        ReflectionTestUtils.setField(config, "localMaxSize", 10);
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 16380);
    }

    @Test
    @DisplayName("Create object memory stash")
    void createMemStash() {
        assertNotNull(config.memStash());
    }

    @Test
    @DisplayName("Create object redis stash")
    void createRedisStash() {
        assertNotNull(config.redisStash());
    }

    @Test
    @DisplayName("Create map memory cache")
    void createMapLocalStash() {
        assertNotNull(config.localMapCache(config.memStash()));
    }

    @Test
    @DisplayName("Create map redis cache")
    void createMapDistrStash() {
        assertNotNull(config.centralizedMapCache(config.redisStash()));
    }

    @Test
    @DisplayName("Create object memory cache")
    void createObjectLocalStash() {
        assertNotNull(config.localObjectCache(config.memStash(), new ObjectMapper()));
    }

    @Test
    @DisplayName("Create object redis cache")
    void createObjectDistrStash() {
        assertNotNull(config.centralizedObjectCache(config.redisStash(),
                new ObjectMapper()));
    }

    @Test
    @DisplayName("Create factory")
    void createFactory() {
        assertNotNull(config.hybridCacheFactory(
                config.localObjectCache(config.memStash(), new ObjectMapper()),
                config.centralizedObjectCache(config.redisStash(), new ObjectMapper()),
                config.localMapCache(config.memStash()),
                config.centralizedMapCache(config.redisStash())
                )
        );
    }
}
