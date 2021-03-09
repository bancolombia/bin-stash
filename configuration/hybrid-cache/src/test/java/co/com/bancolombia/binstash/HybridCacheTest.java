package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.SyncRule;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class HybridCacheTest {

    @Mock
    private ObjectCache<Employee> memObjectStash;

    @Mock
    private ObjectCache<Employee> distObjectSash;

    @Mock
    private MapCache localMapStash;

    @Mock
    private MapCache distributedMapStash;

    @Test
    public void createObjectCache() {
        assertNotNull(new HybridCacheFactory<>(memObjectStash,
                distObjectSash, localMapStash, distributedMapStash).newObjectCache());
    }

    @Test
    public void createMapCache() {
        assertNotNull(new HybridCacheFactory<>(memObjectStash,
                distObjectSash, localMapStash, distributedMapStash).newMapCache());

        SyncRule r1 = (keyExpr, syncType) -> true;
        assertNotNull(new HybridCacheFactory<>(memObjectStash,
                distObjectSash, localMapStash, distributedMapStash).newMapCache(
                Collections.singletonList(r1)
        ));
    }

}
