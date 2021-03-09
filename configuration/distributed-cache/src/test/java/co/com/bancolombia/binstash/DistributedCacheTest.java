package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.Stash;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DistributedCacheTest {

    @Mock
    private Stash redisStash;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void createCache() {
        ObjectCache<Employee> cache = new DistributedCacheFactory<Employee>(redisStash, objectMapper).newObjectCache();
        assert cache != null;
    }

    @Test
    public void createMapCache() {
        MapCache cache = new DistributedCacheFactory(redisStash, objectMapper).newMapCache();
        assert cache != null;
    }
}
