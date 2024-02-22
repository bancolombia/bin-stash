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

```gradle
dependencies {
    implementation 'com.github.bancolombia:bin-stash-local:1.2.3'
}
```

For a centralized (redis) cache only

```gradle
dependencies {
    implementation 'com.github.bancolombia:bin-stash-centralized:1.2.3'
}
```

For an hybrid (local and centralized) cache

```gradle
dependencies {
    implementation 'com.github.bancolombia:bin-stash-hybrid:1.2.3'
}
```

2.	Supported Configuration

```yaml
stash:
  memory:
    expireTime: 60 # 1 minute
    maxSize: 10_000
  redis:
    host: myredis.host
    # Only when connecting to a master/replica
    #replicas: replica.myredis.host, replica2.myredis.host
    port: 6379
    database: 0
    # for rbac access
    username: myuser
    password: mypwd
    useSsl: true
    expireTime: 3600 # 1 hour
```

| Configuration            | Description                                                                                                                                                                                                                                  |
--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
| stash.memory.maxSize     | maximum allowed bytes to store in memory cache                                                                                                                                                                                               |
| stash.memory.expireTime | set maximum time to hold keys in cache (in seconds).<br/> If not defined, a value of 300 seconds is used as default.<br/>Note that `save()` methods that receive a TTL argument, will ignore such value if its greater than `expireTime`.   |
| stash.redis.host         | host to connect to (when connecting to a master-replica cluster this is the master host)                                                                                                                                                     |
| stash.redis.replicas     | host names of replicas, comma separated. (when connecting to a master-replica cluster)                                                                                                                                                       |
| stash.redis.port         | redis port (when connecting to master-replicas will use same port for all hosts)                                                                                                                                                             |
| stash.redis.database     | database number to use on single node (0 default)                                                                                                                                                                                            |
| stash.redis.username     | username (when using RBAC)                                                                                                                                                                                                                   |
| stash.redis.password     | password when using AUTH or RBAC                                                                                                                                                                                                             |
| stash.redis.useSsl       | true or false. Indicates the client to connect to redis via secure connection                                                                                                                                                                |
| stash.redis.expireTime  | default TTL time (in seconds) to hold every key stored in redis. If this parameter is not defined a default value of 300 seconds is used. This value can be overriden for an specific key, with the TTL argument in the `save()` methods. |


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
