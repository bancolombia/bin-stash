package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.config.HybridCacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import redis.embedded.RedisServer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HybridCacheConfigTest {

    private static RedisServer redisServer;
    private HybridCacheConfig config;

    @BeforeAll
    public static void prepare() throws IOException {
        redisServer = new RedisServer(16380);
        redisServer.start();
    }

    @AfterAll
    public static void clean() {
        redisServer.stop();
    }

    @BeforeEach
    public void before() {
        config = new HybridCacheConfig();
        ReflectionTestUtils.setField(config, "localExpireTime", 1);
        ReflectionTestUtils.setField(config, "localMaxSize", 10);
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 16380);
    }

    @Test
    @DisplayName("Create object memory stash")
    public void createMemStash() {
        assertNotNull(config.memStash());
    }

    @Test
    @DisplayName("Create object redis stash")
    public void createRedisStash() {
        assertNotNull(config.redisStash());
    }

    @Test
    @DisplayName("Create map memory cache")
    public void createMapLocalStash() {
        assertNotNull(config.localMapCache(config.memStash()));
    }

    @Test
    @DisplayName("Create map redis cache")
    public void createMapDistrStash() {
        assertNotNull(config.distributedMapCache(config.redisStash()));
    }

    @Test
    @DisplayName("Create object memory cache")
    public void createObjectLocalStash() {
        assertNotNull(config.localObjectCache(config.memStash(), new ObjectMapper()));
    }

    @Test
    @DisplayName("Create object redis cache")
    public void createObjectDistrStash() {
        assertNotNull(config.distributedObjectCache(config.redisStash(),
                new ObjectMapper()));
    }

    @Test
    @DisplayName("Create factory")
    public void createFactory() {
        assertNotNull(config.hybridCacheFactory(
                config.localObjectCache(config.memStash(), new ObjectMapper()),
                config.distributedObjectCache(config.redisStash(), new ObjectMapper()),
                config.localMapCache(config.memStash()),
                config.distributedMapCache(config.redisStash())
                )
        );
    }
}
