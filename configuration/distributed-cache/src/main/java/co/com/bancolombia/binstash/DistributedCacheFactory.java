package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.Stash;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DistributedCacheFactory<V extends Object> {

    private final Stash distributedStash;
    private final ObjectMapper objectMapper;

    public ObjectCache<V> newObjectCache() {
        final SingleTierObjectCacheUseCase<V> cache = new SingleTierObjectCacheUseCase<>(this.distributedStash, objectMapper);
        return cache;
    }

    public MapCache newMapCache() {
        final SingleTierMapCacheUseCase cache = new SingleTierMapCacheUseCase(this.distributedStash);
        return cache;
    }
}
