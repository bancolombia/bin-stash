# Bin-Stash 

Library for caching data:

- In memory or with a distributed cache (redis) making a single tier cache.
- Or using both as a two tier stage cache.

# Getting Started

1.	Installation process

For local cache only

```
<dependency>
  <groupId>co.com.bancolombia.binstash</groupId>
  <artifactId>local-cache</artifactId>
  <version>1.0.0</version>
</dependency>
```

For a distributed (redis) cache only

```
<dependency>
  <groupId>co.com.bancolombia.binstash</groupId>
  <artifactId>distributed-cache</artifactId>
  <version>1.0.0</version>
</dependency>
```

For an hybrid (local and distributed) cache

```
<dependency>
  <groupId>co.com.bancolombia.binstash</groupId>
  <artifactId>hybrid-cache</artifactId>
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

   3.2. For Distributed Cache

    ```java
    @Configuration
    public class DistributedExampleConfiguration {
    
        [...]
    
        @Bean
        public ObjectCache<Person> objectCache(DistributedCacheFactory<> distCacheFactory) {
            new distCacheFactory.newObjectCache();
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
            // Just one rule in this demo. Push local cache key-values to distributed cache, disregarding syncType 
            // (UPSTREAM or DOWNSTREAM) and pull distributed cache key-values to local cache when affected 
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

# Build and Test
TODO: Describe and show how to build your code and run the tests. 

# Contribute
TODO: Explain how other users and developers can contribute to make your code better. 
