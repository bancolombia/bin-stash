# Bin-Stash 

![](https://github.com/bancolombia/bin-stash/workflows/Java%20CI%20with%20Gradle/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=bancolombia_bin-stash&metric=alert_status)](https://sonarcloud.io/dashboard?id=bancolombia_bin-stash)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=bancolombia_bin-stash&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=bancolombia_bin-stash)
[![codecov](https://codecov.io/gh/bancolombia/bin-stash/branch/master/graph/badge.svg)](https://codecov.io/gh/bancolombia/bin-stash)
[![GitHub license](https://img.shields.io/github/license/Naereen/StrapDown.js.svg)](https://github.com/bancolombia/bin-stash/blob/master/LICENSE)

Library for caching data:

- In memory (using Caffeine) or with a centralized cache (Redis using Lettuce Reactive) making a single tier cache.
- Or using both as a two tier stage cache.

# Getting Started

1.	Installation process

For local cache only

```
<dependency>
  <groupId>co.com.bancolombia</groupId>
  <artifactId>bin-stash-local</artifactId>
  <version>1.0.0</version>
</dependency>
```

For a centralized (redis) cache only

```
<dependency>
  <groupId>co.com.bancolombia</groupId>
  <artifactId>bin-stash-centralized</artifactId>
  <version>1.0.0</version>
</dependency>
```

For an hybrid (local and centralized) cache

```
<dependency>
  <groupId>co.com.bancolombia</groupId>
  <artifactId>bin-stash-hybrid</artifactId>
  <version>1.0.0</version>
</dependency>
```

2.	Configuration

```yaml
stash:
  memory:
    expireTime: 10
    maxSize: 10_000
  redis:
    expireTime: 60
    host: myredis.host
    port: 6379
    database: 0
    password: mypwd
```

3. Usage

    3.1. For Local Cache

    ```java
    @Configuration
    public class LocalExampleConfiguration {
    
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    
        @Bean
        public ObjectCache<Person> objectCache(LocalCacheFactory<Person> localCacheFactory) {
            return localCacheFactory.newObjectCache();
        }
    }
    ```

   3.2. For Centralized Cache

    ```java
    @Configuration
    public class CentralizedExampleConfiguration {
    
        [...]
    
        @Bean
        public ObjectCache<Person> objectCache(CentralizedCacheFactory<> centralizedCacheFactory) {
            return centralizedCacheFactory.newObjectCache();
        }
    }
    ```

   3.3. For Hybrid Cache

    ```java
    @Configuration
    public class HybridExampleConfiguration {
    
        [...]
    
        @Bean
        public List<SyncRule> cacheSyncRules() {
            // Just one rule in this demo. Push local cache key-values to centralized cache, disregarding syncType 
            // (UPSTREAM or DOWNSTREAM) and pull centralized cache key-values to local cache when affected 
            // key (keyArg) is ANY string.
            SyncRule simpleSyncRule = (keyArg, syncType) -> true;
            return Collections.singletonList(simpleSyncRule);
        }  
    
        @Bean
        public ObjectCache<Person> objectCache(HybridCacheFactory<Person> cacheFactory,
                                              List<SyncRule> cacheSyncRules) {
           return cacheFactory.newObjectCache(cacheSyncRules);
        }
    }
    ```
   
You can now use `ObjectCache<>` in your app:

```java
@Component
public class PersonCachedHandler {

    public final DummyRepo dummyRepo;
    public final ObjectCache<Person> cache;

    @Autowired
    public PersonCachedHandler(DummyRepo dummyRepo,
                               ObjectCache<Person> cache) {
        this.dummyRepo = dummyRepo;
        this.cache = cache;
    }

    public Mono<ServerResponse> fetchWithCache(ServerRequest request) {

        String key = Optional.of(request.pathVariable("id"))
                .orElse("JhonSmith1");

        // Usage with reactor-extra CacheMono
        Mono<Person> cached = CacheMono
            .lookup(k -> cache.get(k, Person.class) // Get from cache
                     .map(Signal::next), key)
                .onCacheMissResume(dummyRepo.findByName(key)) // Get from some db repo
                .andWriteWith((k, sig) ->
                        cache.save(k, sig.get()).then() // Save to cache
                );

        return cached
                .flatMap(person -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(person))
        );
    }
}
```
3.	Latest releases
4.	API references

# How two tier cache works

When using the cache in hybrid mode (two tier cache), requests works as described:

**GET Operation**

1. When a lookup misses local cache, and upstream sync is allowed, bin-stash tries to lookup in the centralized cache.
2. If the lookup operation in the centralized cache hits a key, then return the value. Also if downtream sync 
   is allowed, the key-value is replicated in local cache.
   
**SAVE operation**

1. Writes are performed in local cache, and if upstream sync is allowed, bin-stash tries to write key-value in the
   centralized cache, given this key doesn't previously exists.

**EVICT operation**

1. `Evict(key)` is performed in local cache, and if upstream sync is allowed, bin-stash tries to evict key-value in the
   centralized cache.
2. `EvictAll` operation it's performed on local and is not syncronized upstream.   

**Sync Rules**

DoubleTierCache uses a collection of `SyncRule` to determine if upstream/downstream sync takes place. 
This functional interface takes keyname and `SyncType` (UPSTREAM or DOWNSTREAM) as arguments, and should return 
a `boolean`.

# Build and Test
TODO: Describe and show how to build your code and run the tests. 

# Contribute
TODO: Explain how other users and developers can contribute to make your code better. 
