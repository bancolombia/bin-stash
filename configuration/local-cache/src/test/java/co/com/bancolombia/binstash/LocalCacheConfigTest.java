package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.config.LocalCacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LocalCacheConfigTest {

    private LocalCacheConfig<Employee> config;

    @BeforeEach
    public void before() {
        config = new LocalCacheConfig<>();
        ReflectionTestUtils.setField(config, "expireTime", 1);
        ReflectionTestUtils.setField(config, "maxSize", 10);
    }

    @Test
    @DisplayName("Create memStash")
    public void createStash() {
        assertNotNull(config.memStash());
    }

    @Test
    @DisplayName("Create factory")
    public void createFactory() {
        assertNotNull(config.localCacheFactory(config.memStash(), new ObjectMapper()));
    }
}
