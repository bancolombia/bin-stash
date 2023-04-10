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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HybridCacheConfig {

    @Value("${stash.memory.expireTime:-1}")
    private int localExpireTime;

    @Value("${stash.memory.maxSize:1000}")
    private int localMaxSize;

    @Value("${stash.redis.host:localhost}")
    private String host;

    @Value("${stash.redis.replicas:}")
    private String replicas;

    @Value("${stash.redis.port:6379}")
    private int port;

    @Value("${stash.redis.database:0}")
    private int database;

    @Value("${stash.redis.username:}")
    private String username;

    @Value("${stash.redis.password:}")
    private String password;

    @Value("${stash.redis.useSsl:false}")
    private boolean useSsl;

    @Value("${stash.redis.expireTime:-1}")
    private int redisExpireTime;

    @Bean(name = "hybridMemStashBean")
    public Stash memStash() {
        return new MemoryStash.Builder()
                .expireAfter(localExpireTime)
                .maxSize(localMaxSize)
                .build();
    }

    @Bean
    public RedisProperties redisProperties() {
        RedisProperties properties = new RedisProperties();
        properties.setHost(this.host);
        properties.setHostReplicas(this.replicas);
        properties.setPort(this.port);
        properties.setUsername(this.username);
        properties.setPassword(this.password);
        properties.setDatabase(this.database);
        properties.setUseSsl(this.useSsl);
        properties.setExpireAfter(this.redisExpireTime);
        return properties;
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
