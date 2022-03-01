package co.com.bancolombia.binstash.demohybrid.config;

import co.com.bancolombia.binstash.demohybrid.handler.PersonCachedHandler;
import co.com.bancolombia.binstash.demohybrid.handler.PersonHandler;
import co.com.bancolombia.binstash.demohybrid.model.Person;
import co.com.bancolombia.binstash.demohybrid.repository.PersonRepo;
import co.com.bancolombia.binstash.HybridCacheFactory;
import co.com.bancolombia.binstash.demohybrid.model.DummyRepo;
import co.com.bancolombia.binstash.model.SyncRule;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Collections;
import java.util.List;

@Configuration
public class HybridExampleConfiguration {

    @Bean
    public DummyRepo dummyRepo() {
        return new PersonRepo();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public List<SyncRule> cacheSyncRules() {
        // Just one rule, in this demo. Push local cache key-values to centralized cache (upstream = true)
        // and pull  key-values from centralized cache (downstream = true)
        // when affected key is ANY string and syncType es either UPSTREAM / DOWNSTREAM
        SyncRule simpleSyncRule = (keyArg, syncType) -> true;
        return Collections.singletonList(simpleSyncRule);
    }

    @Bean
    public ObjectCache<Person> objectCache(HybridCacheFactory<Person> cacheFactory,
                                           List<SyncRule> cacheSyncRules) {
        return cacheFactory.newObjectCache(cacheSyncRules);
    }

    @Bean
    public RouterFunction<ServerResponse> route(PersonHandler personHandler,
                                                PersonCachedHandler personCachedHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET("/nocache/{name}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                            personHandler::fetchNoCache)
                .andRoute(RequestPredicates.GET("/withcache/{name}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                            personCachedHandler::fetchWithCache);
    }

}
