package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.demo.Address;
import co.com.bancolombia.binstash.demo.Person;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoubleTierObjectCacheUseCaseTest {

    private DoubleTierObjectCacheUseCase<Person> cache;

    @Mock
    private RuleEvaluatorUseCase ruleEvaluatorUseCase;

    @Mock
    private ObjectCache<Person> memStash;

    @Mock
    private ObjectCache<Person> redisStash;

    private Person p;

    @BeforeEach
    void before() {
        p = new Person();
        p.setName("Peter Parker");
        p.setAddress(new Address("some-street", "NY"));
        cache = new DoubleTierObjectCacheUseCase<>(memStash, redisStash, ruleEvaluatorUseCase);
    }

    @Test
    @DisplayName("Create cache instance")
    void testCreate() {
        assertNotNull(cache);
    }

    @Test
    @DisplayName("Save on local cache only")
    void testSaveOnlyLocal() {

        when(memStash.save(anyString(), any(Person.class))).thenReturn(Mono.just(p));

        StepVerifier.create(cache.save("pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).save("pparker", p);
        verify(redisStash, times(0)).save("pparker", p);
    }

    @SneakyThrows
    @Test
    @DisplayName("Save on local cache and then update distrubuted")
    void testSaveLocalAndUpstream() {

        when(memStash.save(anyString(), any(Person.class))).thenReturn(Mono.just(p));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(redisStash.exists(anyString())).thenReturn(Mono.just(false));
        when(redisStash.save(anyString(), any(Person.class))).thenReturn(Mono.just(p));

        StepVerifier.create(cache.save("pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).save("pparker", p);
        verify(redisStash, timeout(1000)).exists("pparker");
        verify(redisStash, timeout(1000)).save("pparker", p);
    }

    @Test
    @DisplayName("Get from local cache")
    void testGetFromLocal() {

        when(memStash.get(anyString(), any())).thenReturn(Mono.just(p));

        StepVerifier.create(cache.get("pparker", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).get(eq("pparker"), any());
        verify(memStash, times(0)).save("pparker", p);
        verify(redisStash, times(0)).get(eq("pparker"), any());
        verify(redisStash, times(0)).save("pparker", p);
    }

    @Test
    @DisplayName("Check element exists on local cache")
    void testExist() {

        when(memStash.exists(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.exists("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(memStash).exists("pparker");
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss local cache, then fetch from centralized, but no save local")
    void testGetFromLocalAndUpstreamNotSyncDownstream() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(false);

        when(memStash.get(anyString(), any())).thenReturn(Mono.empty());
        when(redisStash.get(anyString(), any())).thenReturn(Mono.just(p));

        StepVerifier.create(cache.get("pparker", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).get(eq("pparker"), any());
        verify(memStash, times(0)).save("pparker", p);
        verify(redisStash).get(eq("pparker"), any());
        verify(redisStash, times(0)).save("pparker", p);
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss local cache, then fetch from centralized, then sync local cache")
    void testGetFromLocalAndUpstreamAndSyncDownstream() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(true);

        when(memStash.get(anyString(), any())).thenReturn(Mono.empty());
        when(redisStash.get(anyString(), any())).thenReturn(Mono.just(p));
        when(memStash.save(anyString(), any())).thenReturn(Mono.just(p));

        StepVerifier.create(cache.get("pparker", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).get(eq("pparker"), any());
        verify(memStash).save("pparker", p);
        verify(redisStash).get(eq("pparker"), any());
        verify(redisStash, times(0)).save("pparker", p);
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss local cache, then fetch from centralized, then sync local cache II")
    void testGetFromLocalAndUpstreamAndSyncDownstream2() {

        ObjectCache<List<Person>> memStash2 = Mockito.mock(ObjectCache.class);
        ObjectCache<List<Person>> redisStash2 = Mockito.mock(ObjectCache.class);

        DoubleTierObjectCacheUseCase<List<Person>> cache2 =
                new DoubleTierObjectCacheUseCase<>(memStash2, redisStash2, ruleEvaluatorUseCase);

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(true);

        when(memStash2.get(anyString(), any(TypeReference.class))).thenReturn(Mono.empty());
        when(redisStash2.get(anyString(), any(TypeReference.class))).thenReturn(Mono.just(List.of(p)));
        when(memStash2.save(anyString(), any())).thenReturn(Mono.just(List.of(p)));

        StepVerifier.create(cache2.get("pparker", new TypeReference<>(){}))
                .expectSubscription()
                .expectNext(List.of(p))
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash2).get(eq("pparker"), any(TypeReference.class));
        verify(memStash2).save("pparker", List.of(p));
        verify(redisStash2).get(eq("pparker"), any(TypeReference.class));
        verify(redisStash2, times(0)).save("pparker", List.of(p));
    }

    @Test
    @DisplayName("Miss local and centralized caches")
    void testShouldNotGetFromRedis() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(memStash.get(anyString(), any())).thenReturn(Mono.empty());
        when(redisStash.get(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(cache.get("pparker", Person.class))
                .expectSubscription()
                .expectComplete()
                .verify();

        verify(memStash).get(eq("pparker"), any());
        verify(redisStash).get(eq("pparker"), any());
    }

    @Test
    @DisplayName("Get keyset from local")
    void testShoulGetKeyset() {

        when(memStash.keySet()).thenReturn(Mono.just(Set.of("pparker")));

        StepVerifier.create(cache.keySet())
                .expectSubscription()
                .expectNext(Set.of("pparker"))
                .expectComplete()
                .verify();

        verify(memStash).keySet();
    }

    @Test
    @DisplayName("evict key in local cache only")
    void testEvict() {
        when(memStash.evict(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evict("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).evict("pparker");
        verify(redisStash, times(0)).evict("pparker");
    }

    @SneakyThrows
    @Test
    @DisplayName("evict key in local cache then @ centralized")
    void testEvictDobleTier() {
        when(memStash.evict(anyString())).thenReturn(Mono.just(true));
//        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
//        when(redisStash.evict(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evict("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).evict("pparker");
//        verify(redisStash, timeout(1000)).evict("pparker");
    }

    @Test
    @DisplayName("evict all keys")
    void testEvictAll() {

        when(memStash.evictAll()).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evictAll())
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(memStash).evictAll();
    }
}
