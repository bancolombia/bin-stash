package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.Stash;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CentralizedCacheFactory {

    private final Stash centralizedStash;
    private final ObjectMapper objectMapper;

    public <V> ObjectCache<V> newObjectCache() {
        return new SingleTierObjectCacheUseCase<>(this.centralizedStash,
                new SerializatorHelper<>(objectMapper));
    }

    public MapCache newMapCache() {
        return new SingleTierMapCacheUseCase(this.centralizedStash);
    }
}
