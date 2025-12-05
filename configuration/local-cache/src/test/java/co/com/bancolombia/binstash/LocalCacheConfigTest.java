package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.config.LocalCacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalCacheConfigTest {

    private LocalCacheConfig config;

    @BeforeEach
    void before() {
        config = new LocalCacheConfig();
    }

    @Test
    @DisplayName("Create memStash")
    void createStash() {
        assertNotNull(config.memStash(1, 10));
    }

    @Test
    @DisplayName("Create factory")
    void createFactory() {
        assertNotNull(config.localCacheFactory(config.memStash(1, 10), new ObjectMapper()));
    }
}
