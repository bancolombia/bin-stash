package co.com.bancolombia.binstash.config;

import co.com.bancolombia.binstash.HybridCacheFactory;
import co.com.bancolombia.binstash.SingleTierMapCacheUseCase;
import co.com.bancolombia.binstash.SingleTierObjectCacheUseCase;
import co.com.bancolombia.binstash.adapter.memory.MemoryStash;
import co.com.bancolombia.binstash.adapter.redis.RedisStash;
import co.com.bancolombia.binstash.model.api.MapCache;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.Stash;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HybridCacheConfig<V extends Object> {

    @Value("${stash.memory.expireTime:60}")
    private int localExpireTime;

    @Value("${stash.memory.maxSize:1000}")
    private int localMaxSize;

    @Value("${stash.redis.host:localhost}")
    private String host;

    @Value("${stash.redis.port:6379}")
    private int port;

    @Value("${stash.redis.database:0}")
    private int database;

    @Value("${stash.redis.password:}")
    private String password;

    @Value("${stash.redis.expireTime:60}")
    private int redisExpireTime;

    @Bean(name = "hybridMemStashBean")
    public Stash memStash() {
        return new MemoryStash.Builder()
                .expireAfter(localExpireTime)
                .maxSize(localMaxSize)
                .build();
    }

    @Bean(name = "hybridDistStashBean")
    public Stash redisStash() {
        return new RedisStash.Builder()
                .expireAfter(redisExpireTime)
                .host(host)
                .port(port)
                .db(database)
                .password(password)
                .build();
    }

    @Bean(name = "hybridLocalObjCacheBean")
    public ObjectCache<V> localObjectCache(@Qualifier("hybridMemStashBean") Stash memStash,
                                           ObjectMapper objectMapper) {
        return new SingleTierObjectCacheUseCase<V>(memStash, objectMapper);
    }

    @Bean(name = "hybridDistObjCacheBean")
    public ObjectCache<V> distributedObjectCache(@Qualifier("hybridDistStashBean") Stash redisStash,
                                                 ObjectMapper objectMapper) {
        return new SingleTierObjectCacheUseCase<V>(redisStash, objectMapper);
    }

    @Bean(name = "hybridLocalMapCacheBean")
    public MapCache localMapCache(@Qualifier("hybridMemStashBean") Stash memStash) {
        return new SingleTierMapCacheUseCase(memStash);
    }

    @Bean(name = "hybridDistMapCacheBean")
    public MapCache distributedMapCache(@Qualifier("hybridDistStashBean") Stash redisStash) {
        return new SingleTierMapCacheUseCase(redisStash);
    }

    @Bean
    public HybridCacheFactory<V> hybridCacheFactory(@Qualifier("hybridLocalObjCacheBean") ObjectCache<V> localCache,
                                                    @Qualifier("hybridDistObjCacheBean") ObjectCache<V> distributedCache,
                                                    @Qualifier("hybridLocalMapCacheBean") MapCache localMapCache,
                                                    @Qualifier("hybridDistMapCacheBean") MapCache distributedMapCache) {
        return new HybridCacheFactory<V>(localCache, distributedCache,
                localMapCache, distributedMapCache);
    }
}
