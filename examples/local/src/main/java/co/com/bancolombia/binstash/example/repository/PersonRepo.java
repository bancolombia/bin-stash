package co.com.bancolombia.binstash.example.repository;

import co.com.bancolombia.binstash.example.model.DummyRepo;
import co.com.bancolombia.binstash.example.model.Address;
import co.com.bancolombia.binstash.example.model.Person;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Log
@Component
public class PersonRepo implements DummyRepo {

    @Override
    public Mono<Person> findByName(String name) {
        return Mono.just(name)
                .map(name1 -> {
                    log.info("FETCHING INFO FROM DUMMY DATABASE");
                    return new Person("Jhon Smith",
                            new Address("some-street", "NY"));
                })
                // Simulate some latency
                .delaySubscription(Duration.ofMillis(1500));
    }

}
