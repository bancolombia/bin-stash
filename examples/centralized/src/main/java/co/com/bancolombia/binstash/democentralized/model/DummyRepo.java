package co.com.bancolombia.binstash.democentralized.model;

import reactor.core.publisher.Mono;

public interface DummyRepo {
    Mono<Person> findByName(String name);
}
