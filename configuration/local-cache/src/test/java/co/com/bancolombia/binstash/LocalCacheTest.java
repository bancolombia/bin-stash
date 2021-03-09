package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import co.com.bancolombia.binstash.config.LocalCacheConfig;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LocalCacheTest {

    @Mock
    private MemoryStash localStash;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void createCache() {
        LocalCacheConfig<Employee> config = new LocalCacheConfig<>();
        LocalCacheFactory<Employee> factory = config.localCacheFactory(localStash, objectMapper);
        ObjectCache<Employee> cache = factory.newObjectCache();
        assert cache != null;

        MapCache mapCache = factory.newMapCache();
        assert mapCache != null;

    }
}
