package co.com.bancolombia.binstash.adapter.redis;

import co.com.bancolombia.binstash.model.InvalidKeyException;
import co.com.bancolombia.binstash.model.api.Stash;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RedisStash implements Stash {

    private static final String ERROR_KEY_MSG = "Caching key cannot be null";

    private final RedisReactiveCommands<String, String> redisReactiveCommands;
    private final int expireAfter;

    private RedisStash(RedisReactiveCommands<String, String> redisReactiveCommands,
                       int expireAfter) {
        this.redisReactiveCommands = redisReactiveCommands;
        this.expireAfter = expireAfter;
    }

    @Override
    public Mono<String> save(String key, String value) {
        if (StringUtils.isAnyBlank(key, value)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.set(key, value, SetArgs.Builder.ex(this.expireAfter))
                    .map(r -> value);
        }
    }

    @Override
    public Mono<String> get(String key) {
        if (StringUtils.isBlank(key)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.get(key);
        }
    }

    @Override
    public Mono<Set<String>> keySet() {
        return redisReactiveCommands.keys("*").collectList()
                .map(HashSet::new);
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return this.get(key)
                .map(r -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> evict(String key) {
        if (StringUtils.isBlank(key)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.del(key)
                    .map(num -> num > 0);
        }
    }

    @Override
    public Mono<Boolean> evictAll() {
        return redisReactiveCommands.flushdb()
                .map(reply -> true);
    }

    @Override
    public Mono<Map<String, String>> hSave(String key, Map<String, String> value) {
        if (StringUtils.isBlank(key) || value == null) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.hmset(key, value)
                    .map(r -> value);
        }
    }

    @Override
    public Mono<String> hSave(String key, String field, String value) {
        if (StringUtils.isAnyBlank(key, field, value)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.hset(key, field, value)
                    .map(r -> value);
        }
    }

    @Override
    public Mono<String> hGet(String key, String field) {
        if (StringUtils.isAnyBlank(key, field)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.hget(key, field);
        }
    }

    @Override
    public Mono<Map<String, String>> hGetAll(String key) {
        if (StringUtils.isBlank(key)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.hgetall(key)
                    .collectMap(KeyValue::getKey, KeyValue::getValue);
        }
    }

    @Override
    public Mono<Boolean> hDelete(String key, String field) {
        if (StringUtils.isAnyBlank(key, field)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.hdel(key, field).map(count -> count == 1);
        }
    }

    @Override
    public Mono<Boolean> hDelete(String key) {
        if (StringUtils.isBlank(key)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.hgetall(key).map(KeyValue::getKey)
                    .collectList()
                    .map(fields -> fields.toArray(new String[]{}))
                    .flatMap(fields -> redisReactiveCommands.hdel(key, fields))
                    .map(count -> count >= 1);
        }
    }

    public static final class Builder {
        private String host;
        private int port = 6379;
        private int database = 0;
        private String password;
        private int expireAfter = 300; // seconds

        public Builder expireAfter(int seconds) {
            this.expireAfter = seconds;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder db(int db) {
            this.database = db;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        private String buildUrl() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("redis://");
            if (StringUtils.isNotBlank(this.password)) {
                buffer.append(this.password);
                buffer.append("@");
            }
            if (StringUtils.isNotBlank(this.host)) {
                buffer.append(this.host);
            } else {
                buffer.append("localhost");
            }
            buffer.append(":");
            buffer.append(this.port);
            if (this.database > 0) {
                buffer.append("/");
                buffer.append(this.database);
            }
            return buffer.toString();
        }

        public RedisStash build() {
            RedisClient redisClient =
                    RedisClient.create(this.buildUrl());
            RedisReactiveCommands<String, String> commands = redisClient.connect().reactive();
            return new RedisStash(commands, this.expireAfter);
        }
    }
}
