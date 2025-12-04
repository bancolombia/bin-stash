package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import co.com.bancolombia.binstash.config.LocalCacheConfig;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class LocalCacheTest {

    @Mock
    private MemoryStash localStash;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void createCache() {
        LocalCacheConfig config = new LocalCacheConfig();
        LocalCacheFactory factory = config.localCacheFactory(localStash, objectMapper);
        ObjectCache<Employee> cache = factory.newObjectCache();
        assertNotNull(cache);

        MapCache mapCache = factory.newMapCache();
        assertNotNull(mapCache);
    }
}
