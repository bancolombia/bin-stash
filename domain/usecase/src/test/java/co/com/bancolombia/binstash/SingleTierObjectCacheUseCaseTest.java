package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.demo.Address;
import co.com.bancolombia.binstash.demo.Person;
import co.com.bancolombia.binstash.model.InvalidValueException;
import co.com.bancolombia.binstash.model.api.StringStash;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SingleTierObjectCacheUseCaseTest {

    private SingleTierObjectCacheUseCase<Person> cache;

    @Mock
    private StringStash mockedStash;

    private ObjectMapper objectMapper;

    private SerializatorHelper<Person> serializatorHelper;

    private Person p;

    private String serializedPerson;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();

        p = new Person();
        p.setName("Peter Parker");
        p.setAddress(new Address("some-street", "NY"));

        serializedPerson = "";
        try {
            serializedPerson = this.objectMapper.writeValueAsString(p);
        } catch (JacksonException e) {
            e.printStackTrace();
        }

        serializatorHelper = new SerializatorHelper<>(objectMapper);

        cache = new SingleTierObjectCacheUseCase<>(mockedStash, serializatorHelper);
    }

    @AfterEach
    void after() {
        cache.evictAll();
    }

    @Test
    @DisplayName("Create cache")
    void testCreate() {
        assertNotNull(cache);
    }

    @Test
    @DisplayName("save in cache")
    void testSave() {
        assert cache != null;

        when(mockedStash.save(anyString(), anyString(), eq(-1))).thenReturn(Mono.just(serializedPerson));
        when(mockedStash.evictAll()).thenReturn(Mono.just(true));

        StepVerifier.create(cache.save("pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(mockedStash).save("pparker", serializedPerson, -1);
    }

    @Test
    @DisplayName("save in cache (List object)")
    void testSaveList() {

        SerializatorHelper<List<Person>> serializatorHelper2 = new SerializatorHelper<>(objectMapper);

        SingleTierObjectCacheUseCase<List<Person>> cache2 =
                new SingleTierObjectCacheUseCase<>(mockedStash, serializatorHelper2);

        String serializedListOfPerson = "";
        try {
            serializedListOfPerson = this.objectMapper.writeValueAsString(List.of(p));
        } catch (JacksonException e) {
            e.printStackTrace();
        }

        when(mockedStash.save(anyString(), anyString(), eq(-1))).thenReturn(Mono.just(serializedListOfPerson));

        StepVerifier.create(cache2.save("pparker", List.of(p)))
                .expectSubscription()
                .expectNext(List.of(p))
                .expectComplete()
                .verify();

        verify(mockedStash).save("pparker", serializedListOfPerson, -1);
    }


    @Test
    @DisplayName("Get from cache")
    void testGet() {
        assert cache != null;

        when(mockedStash.save(anyString(), anyString(), eq(-1))).thenReturn(Mono.just(serializedPerson));
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

        verify(mockedStash).save("pparker", serializedPerson, -1);
        verify(mockedStash).get("pparker");
    }

    @Test
    @DisplayName("get from cache (List object)")
    void testGetList() {
        SerializatorHelper<List<Person>> serializatorHelper2 = new SerializatorHelper<>(objectMapper);

        SingleTierObjectCacheUseCase<List<Person>> cache2 =
                new SingleTierObjectCacheUseCase<>(mockedStash, serializatorHelper2);

        String serializedListOfPerson = "";
        try {
            serializedListOfPerson = this.objectMapper.writeValueAsString(List.of(p));
        } catch (JacksonException e) {
            e.printStackTrace();
        }

        when(mockedStash.save(anyString(), anyString(), eq(-1))).thenReturn(Mono.just(serializedListOfPerson));
        when(mockedStash.get(anyString())).thenReturn(Mono.just(serializedListOfPerson));

        Mono<List<Person>> persons = cache2.save("persons", List.of(p))
                .then(cache2.get("persons", new TypeReference<List<Person>>() {}));

        StepVerifier.create(persons)
                .expectSubscription()
                .expectNext(List.of(p))
                .expectComplete()
                .verify();

        verify(mockedStash).save("persons", serializedListOfPerson, -1);
        verify(mockedStash).get("persons");
    }

    @Test
    @DisplayName("get from cache (Map object)")
    void testGetMap() {
        SerializatorHelper<Map<String, Person>> serializatorHelper2 = new SerializatorHelper<>(objectMapper);

        SingleTierObjectCacheUseCase<Map<String, Person>> cache2 =
                new SingleTierObjectCacheUseCase<>(mockedStash, serializatorHelper2);

        String serializedMapOfPerson = "";
        try {
            serializedMapOfPerson = this.objectMapper.writeValueAsString(Map.of("p1", p));
        } catch (JacksonException e) {
            e.printStackTrace();
        }

        when(mockedStash.save(anyString(), anyString(), eq(-1))).thenReturn(Mono.just(serializedMapOfPerson));
        when(mockedStash.get(anyString())).thenReturn(Mono.just(serializedMapOfPerson));

        Mono<Map<String, Person>> persons = cache2.save("persons", Map.of("p1", p))
                .then(cache2.get("persons", new TypeReference<Map<String, Person>>() {}))
                .log();

        StepVerifier.create(persons)
                .expectSubscription()
                .expectNext(Map.of("p1", p))
                .expectComplete()
                .verify();

        verify(mockedStash).save("persons", serializedMapOfPerson, -1);
        verify(mockedStash).get("persons");
    }

    @Test
    @DisplayName("Check element exists on cache")
    void testExist() {

        when(mockedStash.exists(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.exists("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).exists("pparker");
    }

    @Test
    @DisplayName("Get keyset")
    void testGetKeyset() {

        when(mockedStash.keySet()).thenReturn(Mono.just(Set.of("pparker")));

        StepVerifier.create(cache.keySet())
                .expectSubscription()
                .expectNext(Set.of("pparker"))
                .expectComplete()
                .verify();

        verify(mockedStash).keySet();
    }

    @Test
    @DisplayName("Should get keys")
    void testKeys() {

        when(mockedStash.keys(anyString(), anyInt())).thenReturn(Flux.just("key1", "key2"));

        StepVerifier.create(cache.keys("k*", 2))
                .expectSubscription()
                .expectNext("key1")
                .expectNext("key2")
                .expectComplete()
                .verify();

        verify(mockedStash).keys("k*", 2);
    }

    @Test
    @DisplayName("Should not get keys")
    void testKeysEmpty() {

        when(mockedStash.keys(anyString(), anyInt())).thenReturn(Flux.empty());

        StepVerifier.create(cache.keys("k*", 2))
                .expectSubscription()
                .expectComplete()
                .verify();

        verify(mockedStash).keys("k*", 2);
    }

    @Test
    @DisplayName("evict key in cache")
    void testEvict() {

        when(mockedStash.evict(anyString())).thenReturn(Mono.just(true));

        Mono<Boolean> personMono = cache.evict("pparker");

        StepVerifier.create(personMono)
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).evict("pparker");
    }

    @Test
    @DisplayName("Set save with TTL in cache")
    void testSetSaveWithTtl() {

        when(mockedStash.setSave(anyString(), anyString(), anyString(), eq(3600)))
                .thenReturn(Mono.just(serializedPerson));

        StepVerifier.create(cache.setSave("user:index", "pparker", p, 3600))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(mockedStash).setSave("user:index", "pparker", serializedPerson, 3600);
    }

    @Test
    @DisplayName("Set save without TTL in cache")
    void testSetSave() {

        when(mockedStash.setSave(anyString(), anyString(), anyString(), eq(-1)))
                .thenReturn(Mono.just(serializedPerson));

        StepVerifier.create(cache.setSave("user:index", "pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(mockedStash).setSave("user:index", "pparker", serializedPerson, -1);
    }

    @Test
    @DisplayName("Set save with TTL in cache (List object)")
    @SneakyThrows
    void testSetSaveListWithTtl() {

        SerializatorHelper<List<Person>> serializatorHelper2 = new SerializatorHelper<>(objectMapper);

        SingleTierObjectCacheUseCase<List<Person>> cache2 =
                new SingleTierObjectCacheUseCase<>(mockedStash, serializatorHelper2);

        String serializedListOfPerson = this.objectMapper.writeValueAsString(List.of(p));
        when(mockedStash.setSave(anyString(), anyString(), anyString(), eq(3600)))
                .thenReturn(Mono.just(serializedListOfPerson));

        StepVerifier.create(cache2.setSave("users:index", "list", List.of(p), 3600))
                .expectSubscription()
                .expectNext(List.of(p))
                .expectComplete()
                .verify();

        verify(mockedStash).setSave("users:index", "list", serializedListOfPerson, 3600);
    }

    @Test
    @DisplayName("Set save without object")
    void testSetSaveWithNullObject() {

        StepVerifier.create(cache.setSave("user:index", "pparker", null, 3600))
                .expectError(InvalidValueException.class)
                .verify();
    }

    @Test
    @DisplayName("Set get all from cache")
    void testSetGetAll() {

        when(mockedStash.setGetAll(anyString()))
                .thenReturn(Flux.just(serializedPerson));

        StepVerifier.create(cache.setGetAll("user:index", Person.class))
                .expectSubscription()
                .expectNextMatches(received -> {
                    assert received.equals(p);
                    return true;
                })
                .expectComplete()
                .verify();

        verify(mockedStash).setGetAll("user:index");
    }

    @Test
    @DisplayName("Set get all from cache with multiple items")
    @SneakyThrows
    void testSetGetAllMultiple() {

        Person p2 = new Person();
        p2.setName("Tony Stark");
        p2.setAddress(new Address("Stark Tower", "NY"));

        String serializedPerson2 = this.objectMapper.writeValueAsString(p2);

        when(mockedStash.setGetAll(anyString()))
                .thenReturn(Flux.just(serializedPerson, serializedPerson2));

        StepVerifier.create(cache.setGetAll("user:index", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectNext(p2)
                .expectComplete()
                .verify();

        verify(mockedStash).setGetAll("user:index");
    }

    @Test
    @DisplayName("Set get all from cache returns empty")
    void testSetGetAllEmpty() {

        when(mockedStash.setGetAll(anyString())).thenReturn(Flux.empty());

        StepVerifier.create(cache.setGetAll("user:index", Person.class))
                .expectSubscription()
                .expectComplete()
                .verify();

        verify(mockedStash).setGetAll("user:index");
    }

    @Test
    @DisplayName("Set remove key from cache")
    void testSetRemove() {

        when(mockedStash.setRemove(anyString(), anyString())).thenReturn(Mono.just(true));

        Mono<Boolean> result = cache.setRemove("user:index", "pparker");

        StepVerifier.create(result)
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).setRemove("user:index", "pparker");
    }

    @Test
    @DisplayName("Set remove key not found in cache")
    void testSetRemoveNotFound() {

        when(mockedStash.setRemove(anyString(), anyString())).thenReturn(Mono.just(false));

        Mono<Boolean> result = cache.setRemove("user:index", "unknown");

        StepVerifier.create(result)
                .expectSubscription()
                .expectNext(false)
                .expectComplete()
                .verify();

        verify(mockedStash).setRemove("user:index", "unknown");
    }

    @Test
    @DisplayName("Unreadable cached value is treated as a cache miss")
    void testGetUnreadableValueIsMiss() {
        when(mockedStash.get(anyString())).thenReturn(Mono.just("{\"address\":\"SECRET-TOKEN\"}"));

        StepVerifier.create(cache.get("pparker", Person.class))
                .expectSubscription()
                .verifyComplete();

        // the stash may be shared with readers that can deserialize the value
        verify(mockedStash, never()).evict(anyString());
    }

    @Test
    @DisplayName("Unreadable cached value is treated as a cache miss (TypeReference)")
    void testGetWithRefUnreadableValueIsMiss() {
        when(mockedStash.get(anyString())).thenReturn(Mono.just("not-json"));

        StepVerifier.create(cache.get("pparker", new TypeReference<Person>() {}))
                .expectSubscription()
                .verifyComplete();

        verify(mockedStash, never()).evict(anyString());
    }

    @Test
    @DisplayName("Set get all skips unreadable members")
    void testSetGetAllSkipsUnreadableMembers() {
        when(mockedStash.setGetAll(anyString())).thenReturn(Flux.just(serializedPerson, "not-json"));

        StepVerifier.create(cache.setGetAll("user:index", Person.class))
                .expectSubscription()
                .expectNext(p)
                .verifyComplete();
    }

    @Test
    @DisplayName("Save fails with InvalidValueException when value cannot be serialized")
    void testSaveUnserializableValue() {
        StepVerifier.create(cache.save("pparker", unserializablePerson(), 60))
                .expectSubscription()
                .expectError(InvalidValueException.class)
                .verify();

        verify(mockedStash, never()).save(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Set save fails with InvalidValueException when value cannot be serialized")
    void testSetSaveUnserializableValue() {
        StepVerifier.create(cache.setSave("user:index", "pparker", unserializablePerson()))
                .expectSubscription()
                .expectError(InvalidValueException.class)
                .verify();

        verify(mockedStash, never()).setSave(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Null target type fails with InvalidValueException without reading the stash")
    void testNullTargetTypeFails() {
        StepVerifier.create(cache.get("pparker", (Class<Person>) null))
                .expectError(InvalidValueException.class)
                .verify();
        StepVerifier.create(cache.get("pparker", (Object) null))
                .expectError(InvalidValueException.class)
                .verify();
        StepVerifier.create(cache.setGetAll("user:index", null))
                .expectError(InvalidValueException.class)
                .verify();

        verify(mockedStash, never()).get(anyString());
        verify(mockedStash, never()).setGetAll(anyString());
    }

    private static Person unserializablePerson() {
        return new Person() {
            @Override
            public String getName() {
                throw new IllegalStateException("boom");
            }
        };
    }

}
