package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalCacheFactory {

    private final MemoryStash memoryStash;
    private final ObjectMapper objectMapper;

    public <V> ObjectCache<V> newObjectCache() {
        return new SingleTierObjectCacheUseCase<>(this.memoryStash,
                new SerializatorHelper<>(this.objectMapper));
    }

    public MapCache newMapCache() {
        return new SingleTierMapCacheUseCase(this.memoryStash);
    }
}
