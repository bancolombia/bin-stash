package co.com.bancolombia.binstash.config;

import co.com.bancolombia.binstash.HybridCacheFactory;
import co.com.bancolombia.binstash.SerializatorHelper;
import co.com.bancolombia.binstash.SingleTierMapCacheUseCase;
import co.com.bancolombia.binstash.SingleTierObjectCacheUseCase;
import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import co.com.bancolombia.binstash.adapter.redis.RedisProperties;
import co.com.bancolombia.binstash.adapter.redis.RedisStashFactory;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.Stash;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class HybridCacheConfig {

    @Bean
    @ConfigurationProperties(prefix = "stash.redis")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    @Bean(name = "hybridMemStashBean")
    public Stash memStash(@Value("${stash.memory.expireTime:-1}") int localExpireTime,
                          @Value("${stash.memory.maxSize:10000}") int localMaxSize) {
        return new MemoryStash.Builder()
                .expireAfter(localExpireTime)
                .maxSize(localMaxSize)
                .build();
    }

    @Bean(name = "hybridCentralStashBean")
    public Stash redisStash(RedisProperties redisProperties) {
        return RedisStashFactory.redisStash(redisProperties);
    }

    @Bean(name = "hybridLocalObjCacheBean")
    public <V> ObjectCache<V> localObjectCache(@Qualifier("hybridMemStashBean") Stash memStash,
                                           ObjectMapper objectMapper) {
        return new SingleTierObjectCacheUseCase<>(memStash,
                new SerializatorHelper<>(objectMapper));
    }

    @Bean(name = "hybridCentralObjCacheBean")
    public <V> ObjectCache<V> centralizedObjectCache(@Qualifier("hybridCentralStashBean") Stash redisStash,
                                                 ObjectMapper objectMapper) {
        return new SingleTierObjectCacheUseCase<>(redisStash,
                new SerializatorHelper<>(objectMapper));
    }

    @Bean(name = "hybridLocalMapCacheBean")
    public MapCache localMapCache(@Qualifier("hybridMemStashBean") Stash memStash) {
        return new SingleTierMapCacheUseCase(memStash);
    }

    @Bean(name = "hybridCentralMapCacheBean")
    public MapCache centralizedMapCache(@Qualifier("hybridCentralStashBean") Stash redisStash) {
        return new SingleTierMapCacheUseCase(redisStash);
    }

    @Bean
    public <V> HybridCacheFactory<V> hybridCacheFactory(@Qualifier("hybridLocalObjCacheBean") ObjectCache<V> localObjectCache,
                                                    @Qualifier("hybridCentralObjCacheBean") ObjectCache<V> centralizedObjectCache,
                                                    @Qualifier("hybridLocalMapCacheBean") MapCache localMapCache,
                                                    @Qualifier("hybridCentralMapCacheBean") MapCache centralizedMapCache) {
        return new HybridCacheFactory<>(localObjectCache, centralizedObjectCache,
                localMapCache, centralizedMapCache);
    }
}
