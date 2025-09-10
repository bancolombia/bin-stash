package co.com.bancolombia.binstash.adapter.redis;

import co.com.bancolombia.binstash.model.InvalidKeyException;
import co.com.bancolombia.binstash.model.api.Stash;
import io.lettuce.core.KeyValue;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisStash implements Stash {

    private static final String ERROR_KEY_MSG = "Caching key cannot be null";
    private static final String INVALID_PATTERN_MSG = "Invalid pattern for keys";

    private static final int DEFAULT_PER_KEY_EXPIRATION_SECONDS = 300;

    private final RedisReactiveCommands<String, String> redisReactiveCommands;

    private final int expireAfter;

    RedisStash(RedisReactiveCommands<String, String> redisReactiveCommands,
                       int expireAfter) {
        this.redisReactiveCommands = redisReactiveCommands;
        this.expireAfter = expireAfter;
    }

    @Override
    public Mono<String> save(String key, String value, int ttl) {
        if (StringUtils.isAnyBlank(key, value)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.set(key, value, SetArgs.Builder.ex(computeTtl(ttl)))
                    .map(r -> value);
        }
    }

    @Override
    public Mono<String> save(String key, String value) {
        return save(key, value, this.expireAfter); // with default expire ttl
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
    public Flux<String> keys(String pattern, int limit) {
        if (StringUtils.isBlank(pattern)) {
            return Flux.error(new IllegalArgumentException(INVALID_PATTERN_MSG));
        }
        final ScanArgs scanArgs = new ScanArgs().match(pattern).limit(limit <= 0 ? Long.MAX_VALUE : limit);
        final int[] emitted = {0};
        return scanRecursive("0", scanArgs, emitted, limit);
    }

    private Flux<String> scanRecursive(String cursor, ScanArgs scanArgs, int[] emitted, int limit) {
        return Flux.defer(() ->
                redisReactiveCommands.scan(ScanCursor.of(cursor), scanArgs)
                        .flatMapMany(scanResult -> {
                            List<String> keys = scanResult.getKeys();
                            emitted[0] += keys.size();
                            Flux<String> currentBatch = Flux.fromIterable(keys);
                            if (scanResult.isFinished() || emitted[0] >= limit) {
                                return currentBatch;
                            }
                            return currentBatch.concatWith(
                                    scanRecursive(scanResult.getCursor(), scanArgs, emitted, limit)
                            );
                        })
        );
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
        return hSave(key, value, DEFAULT_PER_KEY_EXPIRATION_SECONDS);
    }

    @Override
    public Mono<Map<String, String>> hSave(String key, Map<String, String> value, int ttl) {
        if (StringUtils.isBlank(key) || value == null) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.hmset(key, value)
                    .zipWith(redisReactiveCommands.expire(key, computeTtl(ttl)))
                    .map(r -> value);
        }
    }

    @Override
    public Mono<String> hSave(String key, String field, String value) {
        return hSave(key, field, value, DEFAULT_PER_KEY_EXPIRATION_SECONDS);
    }

    @Override
    public Mono<String> hSave(String key, String field, String value, int ttl) {
        if (StringUtils.isAnyBlank(key, field, value)) {
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            return redisReactiveCommands.hset(key, field, value)
                    .zipWith(redisReactiveCommands.expire(key, computeTtl(ttl)))
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

    private int computeTtl(int cadidateTtl) {
        int computed;
        if (cadidateTtl > 0) {
            computed = cadidateTtl;
        }
        else if (cadidateTtl < 0 && this.expireAfter > 0) {
            computed = this.expireAfter;
        }
        else {
            computed = DEFAULT_PER_KEY_EXPIRATION_SECONDS;
        }
        return computed;
    }

}
