package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalCacheFactory<V extends Object> {

    private final MemoryStash memoryStash;
    private final ObjectMapper objectMapper;

    public ObjectCache<V> newObjectCache() {
        final SingleTierObjectCacheUseCase<V> cache = new SingleTierObjectCacheUseCase<>(this.memoryStash,
                this.objectMapper);
        return cache;
    }

    public MapCache newMapCache() {
        final SingleTierMapCacheUseCase cache = new SingleTierMapCacheUseCase(this.memoryStash);
        return cache;
    }
}
