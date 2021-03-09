package co.com.bancolombia.binstash.config;

import co.com.bancolombia.binstash.LocalCacheFactory;
import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalCacheConfig<V extends Object> {

    @Value("${stash.memory.expireTime:60}")
    private int expireTime;

    @Value("${stash.memory.maxSize:1000}")
    private int maxSize;

    @Bean(name = "localMemStashBean")
    public MemoryStash memStash() {
        return new MemoryStash.Builder()
                .expireAfter(expireTime)
                .maxSize(maxSize)
                .build();
    }

    @Bean
    public LocalCacheFactory<V> localCacheFactory(@Qualifier("localMemStashBean") MemoryStash memStash,
                                                  ObjectMapper objectMapper) {
        LocalCacheFactory<V> factory = new LocalCacheFactory<>(memStash, objectMapper);
        return factory;
    }
}
