package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.demo.Address;
import co.com.bancolombia.binstash.demo.Person;
import co.com.bancolombia.binstash.model.api.StringStash;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SingleTierObjectCacheUseCaseTest {

    private SingleTierObjectCacheUseCase<Person> cache;

    @Mock
    private StringStash mockedStash;

    private ObjectMapper objectMapper;

    private Person p;

    private String serializedPerson;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper();

        p = new Person();
        p.setName("Peter Parker");
        p.setAddress(new Address("some-street", "NY"));

        serializedPerson = "";
        try {
            serializedPerson = this.objectMapper.writeValueAsString(p);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        cache = new SingleTierObjectCacheUseCase<>(mockedStash, objectMapper);
    }

    @AfterEach
    public void after() {
        cache.evictAll();
    }

    @Test
    @DisplayName("Create cache")
    public void testCreate() {
        assert cache != null;
    }

    @Test
    @DisplayName("save in cache")
    public void testSave() {
        assert cache != null;

        when(mockedStash.save(anyString(), anyString())).thenReturn(Mono.just(serializedPerson));
        when(mockedStash.evictAll()).thenReturn(Mono.just(true));

        StepVerifier.create(cache.save("pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(mockedStash).save(eq("pparker"), eq(serializedPerson));
    }

    @Test
    @DisplayName("Get from cache")
    public void testGet() {
        assert cache != null;

        when(mockedStash.save(anyString(), anyString())).thenReturn(Mono.just(serializedPerson));
        when(mockedStash.get(anyString())).thenReturn(Mono.just(serializedPerson));

        Mono<Person> personMono = cache.save("pparker", p)
                .then(cache.get("pparker", Person.class));

        StepVerifier.create(personMono)
                .expectSubscription()
                .expectNextMatches(received -> {
                    assert received.equals(p);
                    return true;
                })
                .expectComplete()
                .verify();

        verify(mockedStash).save(eq("pparker"), eq(serializedPerson));
        verify(mockedStash).get(eq("pparker"));
    }

    @Test
    @DisplayName("Check element exists on cache")
    public void testExist() {

        when(mockedStash.exists(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.exists("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).exists(eq("pparker"));
    }

    @Test
    @DisplayName("evict key in cache")
    public void testEvict() {

        when(mockedStash.evict(anyString())).thenReturn(Mono.just(true));

        Mono<Boolean> personMono = cache.evict("pparker");

        StepVerifier.create(personMono)
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).evict(eq("pparker"));
    }

}
