package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.Stash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class CentralizedCacheTest {

    @Mock
    private Stash redisStash;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void createCache() {
        ObjectCache<Employee> cache = new CentralizedCacheFactory(redisStash, objectMapper).newObjectCache();
        assertNotNull(cache);
    }

    @Test
    void createMapCache() {
        MapCache cache = new CentralizedCacheFactory(redisStash, objectMapper).newMapCache();
        assertNotNull(cache);
    }
}
