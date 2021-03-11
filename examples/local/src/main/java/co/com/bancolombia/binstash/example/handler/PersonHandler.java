package co.com.bancolombia.binstash.example.handler;

import co.com.bancolombia.binstash.example.model.DummyRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class PersonHandler {

    public final DummyRepo dummyRepo;

    @Autowired
    public PersonHandler(DummyRepo dummyRepo) {
        this.dummyRepo = dummyRepo;
    }

    public Mono<ServerResponse> fetchNoCache(ServerRequest request) {
        String key = Optional.of(request.pathVariable("name"))
                .orElse("Jhon Smith");
        return this.dummyRepo.findByName(key).flatMap(person ->
                ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(person))
        );
    }

}
