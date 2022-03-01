package co.com.bancolombia.binstash.config;

import co.com.bancolombia.binstash.CentralizedCacheFactory;
import co.com.bancolombia.binstash.adapter.redis.RedisStash;
import co.com.bancolombia.binstash.model.api.Stash;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CentralizedCacheConfig<V extends Object> {

    @Value("${stash.redis.host:localhost}")
    private String host;

    @Value("${stash.redis.port:6379}")
    private int port;

    @Value("${stash.redis.database:0}")
    private int database;

    @Value("${stash.redis.password:}")
    private String password;

    @Value("${stash.redis.expireTime:-1}")
    private int redisExpireTime;

    @Bean(name = "centralMemStashBean")
    public Stash redisStash() {
        return new RedisStash.Builder()
                .expireAfter(redisExpireTime)
                .host(host)
                .port(port)
                .db(database)
                .password(password)
                .build();
    }

    @Bean
    public CentralizedCacheFactory<V> newFactory(@Qualifier("centralMemStashBean") Stash centralizedStash,
                                                 ObjectMapper objectMapper) {
        return new CentralizedCacheFactory<>(centralizedStash, objectMapper);
    }
}
