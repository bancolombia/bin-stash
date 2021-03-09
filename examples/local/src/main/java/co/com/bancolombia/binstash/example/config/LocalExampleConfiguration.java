package co.com.bancolombia.binstash.example.config;

import co.com.bancolombia.binstash.example.handler.PersonCachedHandler;
import co.com.bancolombia.binstash.example.model.DummyRepo;
import co.com.bancolombia.binstash.LocalCacheFactory;
import co.com.bancolombia.binstash.example.handler.PersonHandler;
import co.com.bancolombia.binstash.example.model.Person;
import co.com.bancolombia.binstash.example.repository.PersonRepo;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class LocalExampleConfiguration {

    @Bean
    public DummyRepo dummyRepo() {
        return new PersonRepo();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ObjectCache<Person> objectCache(LocalCacheFactory<Person> localCacheFactory) {
        return localCacheFactory.newObjectCache();
    }

    @Bean
    public RouterFunction<ServerResponse> route(PersonHandler personHandler,
                                                PersonCachedHandler personCachedHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET("/nocache/:name")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                            personHandler::fetchNoCache)
                .andRoute(RequestPredicates.GET("/withcache/{name}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                            personCachedHandler::fetchWithCache);
    }

}
