package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.config.DistributedCacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import redis.embedded.RedisServer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

public class DistributedCacheConfigTest {

    private static RedisServer redisServer;
    private DistributedCacheConfig config;

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
        config = new DistributedCacheConfig();
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 16379);
    }

    @Test
    @DisplayName("Create redis stash")
    public void createStash() {
        assertNotNull(config.redisStash());
    }

    @Test
    @DisplayName("Create factory")
    public void createFactory() {
        assertNotNull(config.newFactory(config.redisStash(), new ObjectMapper()));
    }
}
