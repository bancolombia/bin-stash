package co.com.bancolombia.binstash.config;

import co.com.bancolombia.binstash.CentralizedCacheFactory;
import co.com.bancolombia.binstash.adapter.redis.RedisProperties;
import co.com.bancolombia.binstash.adapter.redis.RedisStashFactory;
import co.com.bancolombia.binstash.model.api.Stash;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CentralizedCacheConfig {

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
