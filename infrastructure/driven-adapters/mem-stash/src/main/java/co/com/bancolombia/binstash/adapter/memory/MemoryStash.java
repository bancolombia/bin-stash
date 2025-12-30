package co.com.bancolombia.binstash.adapter.memory;

import co.com.bancolombia.binstash.model.InvalidKeyException;
import co.com.bancolombia.binstash.model.InvalidValueException;
import co.com.bancolombia.binstash.model.api.Stash;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MemoryStash implements Stash {

    private static final int DEFAULT_BASE_EXPIRATION_SECONDS = Integer.MAX_VALUE;
    private static final int DEFAULT_PER_KEY_EXPIRATION_SECONDS = 300;
    private static final String ERROR_KEY_MSG = "Caching key cannot be null";
    private static final String ERROR_VALUE_MSG = "Caching empty or null value not allowed";
    private static final String KEY_SEP = "#";
    private final ConcurrentHashMap<String, Set<String>> indexKeyMap = new ConcurrentHashMap<>();

    private final Cache<String, MemoryStash.Entry> caffeineCache;
    private final int expireAfter;

    private MemoryStash(Cache<String, MemoryStash.Entry> caffeineCache, int expireAfter) {
        this.caffeineCache = caffeineCache;
        this.expireAfter = expireAfter;
    }

    @Override
    public Mono<String> save(String key, String value) {
        return save(key, value, -1);
    }

    @Override
    public Mono<String> save(String key, String value, int ttl) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key))
                throw new InvalidKeyException(ERROR_KEY_MSG);
            caffeineCache.put(key, new Entry(value, computeTtl(ttl)));
            return value;
        });
    }

    @Override
    public Mono<Map<String, String>> hSave(String key, Map<String, String> value) {
        return hSave(key, value, -1);
    }

    public Mono<Map<String, String>> hSave(String key, Map<String, String> value, int ttl) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key)) {
                throw new InvalidKeyException(ERROR_KEY_MSG);
            }
            if (value == null || value.isEmpty()) {
                throw new InvalidValueException(ERROR_VALUE_MSG);
            }
            value.forEach((name, item) -> caffeineCache.put(key + KEY_SEP + name,
                    new Entry(item, computeTtl(ttl))));
            return value;
        });
    }

    @Override
    public Mono<String> hSave(String key, String name, String value) {
        return hSave(key, name, value, -1);
    }

    public Mono<String> hSave(String key, String name, String value, int ttl) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isAnyBlank(key, name)) {
                throw new InvalidKeyException(ERROR_KEY_MSG);
            }
            if (StringUtils.isBlank(value)) {
                throw new InvalidValueException(ERROR_KEY_MSG);
            }
            caffeineCache.put(key + KEY_SEP + name,
                    new Entry(value, computeTtl(ttl)));
            return value;
        });
    }

    @Override
    public Mono<String> get(String key) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key))
                throw new InvalidKeyException(ERROR_KEY_MSG);
            return caffeineCache.getIfPresent(key);
        })
        .filter(entry -> !entry.amIExpired(System.currentTimeMillis()))
        .map(Entry::getData);
    }

    @Override
    public Mono<Set<String>> keySet() {
        long currentTime = System.currentTimeMillis();
        return Mono.fromSupplier(() -> caffeineCache.asMap().entrySet()
                .stream()
                .filter(e -> !e.getValue().amIExpired(currentTime))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet())
        );
    }

    @Override
    public Flux<String> keys(String pattern, int limit) {
        long currentTime = System.currentTimeMillis();
        String curatedPattern = StringUtils.isBlank(pattern) ? ".*" : pattern.replace("*", ".*");
        return Flux.fromStream(() -> caffeineCache.asMap().entrySet()
                .stream()
                .filter(e -> !e.getValue().amIExpired(currentTime))
                .filter(e -> e.getKey().matches(curatedPattern))
                .map(Map.Entry::getKey)
                .limit(limit <= 0 ? Integer.MAX_VALUE : limit)
        );
    }

    @Override
    public Mono<String> hGet(String key, String name) {
        if (StringUtils.isAnyBlank(key, name))
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        return this.get(key + KEY_SEP + name);
    }

    @Override
    public Mono<Map<String, String>> hGetAll(String key) {
        if (StringUtils.isBlank(key))
            return Mono.error(new InvalidKeyException(ERROR_KEY_MSG));
        else {
            final String prefix = key + KEY_SEP;
            return keySet().map(keys -> keys.stream().filter(k -> k.startsWith(prefix)).toList())
                    .map(keys -> Tuples.of(keys, caffeineCache.getAllPresent(keys)))
                    .map(tuple2 -> {
                        final Map<String, String> newMap = new HashMap<>();
                        tuple2.getT1().forEach(k -> {
                            final String _k = k.substring(prefix.length());
                            newMap.put(_k, tuple2.getT2().get(k).getData());
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
        return this.evict(key + KEY_SEP + name);
    }

    @Override
    public Mono<Boolean> hDelete(String key) {
        final String prefix = key + KEY_SEP;
        return Mono.fromSupplier(() -> {
            if (StringUtils.isBlank(key))
                throw new InvalidKeyException(ERROR_KEY_MSG);
            return caffeineCache.asMap().keySet().stream()
                    .filter(k -> k.startsWith(prefix))
                    .toList();
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

    @Override
    public Mono<String> setSave(String indexKey, String key, String value, int ttl) {
        return Mono.fromSupplier( () ->{
            if (StringUtils.isAnyBlank(indexKey, key, value)) {
                throw new InvalidKeyException(ERROR_KEY_MSG);
            } else {
                caffeineCache.put(key, new Entry(value, computeTtl(ttl)));
                indexKeyMap.computeIfAbsent(indexKey, k ->
                        ConcurrentHashMap.newKeySet()).add(key);
                return value;
            }
        });
    }

    @Override
    public Mono<String> setSave(String indexKey, String key, String value) {
        return setSave(indexKey, key, value, -1);
    }

    @Override
    public Flux<String> setGetAll(String indexKey) {
        if (StringUtils.isAnyBlank(indexKey)) {
            return Flux.error(new InvalidKeyException(ERROR_KEY_MSG));
        } else {
            Set<String> keys = indexKeyMap.getOrDefault(indexKey, Set.of());
            return Flux.fromIterable(keys)
                    .flatMap(key -> {
                        Entry entry = caffeineCache.getIfPresent(key);
                        if (entry != null ) {
                            return Mono.just(entry.getData());
                        } else {
                            indexKeyMap.get(indexKey).remove(key);
                            return Mono.empty();
                        }
                    });
        }
    }

    @Override
    public Mono<Boolean> setRemove(String indexKey, String key) {
        return Mono.fromSupplier(() -> {
            if (StringUtils.isAnyBlank(indexKey, key)) {
                throw new InvalidKeyException(ERROR_KEY_MSG);
            } else {
                caffeineCache.invalidate(key);
                return indexKeyMap.getOrDefault(indexKey, Set.of()).remove(key);
            }
        });
    }

    @Data
    public static final class Entry {
        private String data;
        private long expiresAt;

        public Entry(String data, int ttlSecods) {
            this.data = data;
            this.expiresAt = System.currentTimeMillis() + (ttlSecods * 1_000L);
        }

        public boolean amIExpired(long timestamp) {
            return timestamp > this.expiresAt;
        }
    }

    public static final class Builder {
        private int expireAfter = -1; // seconds
        private int maxSize = 1_000;

        public Builder expireAfter(int seconds) {
            this.expireAfter  = seconds;
            return this;
        }

        public Builder maxSize(int size) {
            this.maxSize = size;
            return this;
        }

        public MemoryStash build() {
            return new MemoryStash(
                    Caffeine.newBuilder()
                            .maximumSize(this.maxSize)
                            .expireAfterWrite((this.expireAfter<=0) ?
                                    DEFAULT_BASE_EXPIRATION_SECONDS:this.expireAfter, TimeUnit.SECONDS)
                            .build(),
                    this.expireAfter
            );
        }
    }
}
