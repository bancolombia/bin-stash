package co.com.bancolombia.binstash.example.handler;

import co.com.bancolombia.binstash.example.model.DummyRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class PersonHandler {

    public final DummyRepo dummyRepo;

    @Autowired
    public PersonHandler(DummyRepo dummyRepo) {
        this.dummyRepo = dummyRepo;
    }

    public Mono<ServerResponse> fetchNoCache(ServerRequest request) {
        return this.dummyRepo.findByName("some-name").flatMap(person -> {
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(person));
        });
    }

}
