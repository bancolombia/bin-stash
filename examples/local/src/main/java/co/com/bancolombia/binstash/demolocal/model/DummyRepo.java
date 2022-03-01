package co.com.bancolombia.binstash.demolocal.model;

import reactor.core.publisher.Mono;

public interface DummyRepo {
    Mono<Person> findByName(String name);
}
