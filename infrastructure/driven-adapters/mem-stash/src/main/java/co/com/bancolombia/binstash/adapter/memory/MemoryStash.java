package co.com.bancolombia.binstash.adapter.memory;

import co.com.bancolombia.binstash.model.InvalidKeyException;
import co.com.bancolombia.binstash.model.api.Stash;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MemoryStash implements Stash {

    private static final String ERROR_KEY_MSG = "Caching key cannot be null";

    private final Cache<String, String> caffeineCache;

    private MemoryStash(Cache<String, String> caffeineCache) {
        this.caffeineCache = caffeineCache;
    }

    @Override
    public Mono<String> save(String key, String value) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key))
                throw new InvalidKeyException(ERROR_KEY_MSG);
            caffeineCache.put(key, value);
            return value;
        });
    }

    @Override
    public Mono<Map<String, String>> hSave(String key, Map<String, String> value) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key) || value == null || value.isEmpty())
                throw new InvalidKeyException(ERROR_KEY_MSG);
            value.forEach((name, item) -> caffeineCache.put(key+"-"+name, item));
            return value;
        });
    }

    @Override
    public Mono<String> hSave(String key, String name, String value) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isAnyBlank(key, name, value))
                throw new InvalidKeyException(ERROR_KEY_MSG);
            caffeineCache.put(key+"-"+name, value);
            return value;
        });
    }

    @Override
    public Mono<String> get(String key) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key))
                throw new InvalidKeyException(ERROR_KEY_MSG);
            return caffeineCache.getIfPresent(key);
        });
    }

    @Override
    public Mono<Set<String>> keySet() {
        return Mono.just(caffeineCache.asMap().keySet());
    }

    @Override
    public Mono<String> hGet(String key, String name) {
        if (StringUtils.isAnyBlank(key, name))
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        return this.get(key+"-"+name);
    }

    @Override
    public Mono<Map<String, String>> hGetAll(String key) {
        if (StringUtils.isBlank(key))
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        else {
            final String prefix = key+"-";
            return Mono.just(prefix)
                    .map(prefix1 -> caffeineCache.asMap().keySet().stream()
                            .filter(k -> k.startsWith(prefix1))
                            .collect(Collectors.toList()))
                    .map(keys -> Tuples.of(keys, caffeineCache.getAllPresent(keys)))
                    .map(tuple2 -> {
                        final Map<String, String> newMap = new HashMap<>();
                        tuple2.getT1().forEach(k -> {
                            final String _k = k.substring(prefix.length());
                            newMap.put(_k, tuple2.getT2().get(k));
                        });
                        return newMap;
                    })
                    .filter(dataMap -> !dataMap.isEmpty());
        }
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return this.get(key)
                .map(r -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> evict(String key) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key))
                return false;
            caffeineCache.invalidate(key);
            return true;
        });
    }

    @Override
    public Mono<Boolean> hDelete(String key, String name) {
        if (StringUtils.isAnyBlank(key, name))
            return Mono.just(false);
        return this.evict(key+"-"+name);
    }

    @Override
    public Mono<Boolean> hDelete(String key) {
        final String prefix = key+"-";
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key))
                throw new InvalidKeyException(ERROR_KEY_MSG);
            return caffeineCache.asMap().keySet().stream()
                    .filter(k -> k.startsWith(prefix))
                    .collect(Collectors.toList());
        }).flatMapMany(Flux::fromIterable)
        .flatMap(this::evict)
        .all(result -> result)
        .defaultIfEmpty(true);
    }

    @Override
    public Mono<Boolean> evictAll() {
        return Mono.fromSupplier(() -> {
            caffeineCache.invalidateAll();
            return true;
        });
    }

    public static final class Builder {
        private int expireAfter = 300; // seconds
        private int maxSize = 1_000;

        public Builder expireAfter(int seconds) {
            this.expireAfter = seconds;
            return this;
        }

        public Builder maxSize(int size) {
            this.maxSize = size;
            return this;
        }

        public MemoryStash build() {
            Cache<String, String> cacheImpl = Caffeine.newBuilder()
                .maximumSize(this.maxSize)
                .expireAfterWrite(this.expireAfter, TimeUnit.SECONDS)
                .build();
            return new MemoryStash(cacheImpl);
        }
    }
}
