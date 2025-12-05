package co.com.bancolombia.binstash.config;

import co.com.bancolombia.binstash.CentralizedCacheFactory;
import co.com.bancolombia.binstash.adapter.redis.RedisProperties;
import co.com.bancolombia.binstash.adapter.redis.RedisStashFactory;
import co.com.bancolombia.binstash.model.api.Stash;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class CentralizedCacheConfig {

    @Bean
    @ConfigurationProperties(prefix = "stash.redis")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    @Bean(name = "centralMemStashBean")
    public Stash redisStash(RedisProperties redisProperties) {
        return RedisStashFactory.redisStash(redisProperties);
    }

    @Bean
    public CentralizedCacheFactory newFactory(@Qualifier("centralMemStashBean") Stash centralizedStash,
                                              ObjectMapper objectMapper) {
        return new CentralizedCacheFactory(centralizedStash, objectMapper);
    }
}
