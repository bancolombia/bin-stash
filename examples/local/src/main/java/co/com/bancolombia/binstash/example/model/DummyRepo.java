package co.com.bancolombia.binstash.example.model;

import reactor.core.publisher.Mono;

public interface DummyRepo {
    Mono<Person> findByName(String name);
}
