package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.config.CentralizedCacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import redis.embedded.RedisServer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

class CentralizedCacheConfigTest {

    private static RedisServer redisServer;
    private CentralizedCacheConfig config;

    @BeforeAll
    static void prepare() throws IOException {
        redisServer = new RedisServer(16379);
        redisServer.start();
    }

    @AfterAll
    static void clean() {
        redisServer.stop();
    }

    @BeforeEach
    void before() {
        config = new CentralizedCacheConfig();
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 16379);
    }

    @Test
    @DisplayName("Create redis stash")
    void createStash() {
        assertNotNull(config.redisStash());
    }

    @Test
    @DisplayName("Create factory")
    void createFactory() {
        assertNotNull(config.newFactory(config.redisStash(), new ObjectMapper()));
    }
}
