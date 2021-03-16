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
class HybridCacheTest {

    @Mock
    private ObjectCache<Employee> memObjectStash;

    @Mock
    private ObjectCache<Employee> centralizedObjectSash;

    @Mock
    private MapCache localMapStash;

    @Mock
    private MapCache centralizedMapStash;

    @Test
    void createObjectCache() {
        assertNotNull(new HybridCacheFactory<>(memObjectStash,
                centralizedObjectSash, localMapStash, centralizedMapStash).newObjectCache());
    }

    @Test
    void createMapCache() {
        assertNotNull(new HybridCacheFactory<>(memObjectStash,
                centralizedObjectSash, localMapStash, centralizedMapStash).newMapCache());

        SyncRule r1 = (keyExpr, syncType) -> true;
        assertNotNull(new HybridCacheFactory<>(memObjectStash,
                centralizedObjectSash, localMapStash, centralizedMapStash).newMapCache(
                Collections.singletonList(r1)
        ));
    }

}
