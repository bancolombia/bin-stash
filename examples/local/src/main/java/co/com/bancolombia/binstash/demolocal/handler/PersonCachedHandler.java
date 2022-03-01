package co.com.bancolombia.binstash.demolocal.handler;

import co.com.bancolombia.binstash.demolocal.model.DummyRepo;
import co.com.bancolombia.binstash.demolocal.model.Person;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.Optional;

@Log
@Component
public class PersonCachedHandler {

    private static final int EXPIRE_AFTER = 10;

    public final DummyRepo dummyRepo;
    public final ObjectCache<Person> objectCache;

    @Autowired
    public PersonCachedHandler(DummyRepo dummyRepo,
                               ObjectCache<Person> objectCache) {
        this.dummyRepo = dummyRepo;
        this.objectCache = objectCache;
    }

    public Mono<ServerResponse> fetchWithCache(ServerRequest request) {

        String key = Optional.of(request.pathVariable("name"))
                .orElse("Jhon Smith");

        Mono<Person> cached = CacheMono
            .lookup(k -> objectCache.get(k, Person.class)
                     .map(Signal::next), key)
                .onCacheMissResume(dummyRepo.findByName(key))
                // save in cache and expire after 10 seconds
                .andWriteWith((k, sig) -> objectCache.save(k, sig.get(), EXPIRE_AFTER).then());

        return cached
                .flatMap(person -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(person))
        );
    }
}
