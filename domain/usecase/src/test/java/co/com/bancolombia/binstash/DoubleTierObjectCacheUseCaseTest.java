package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.demo.Address;
import co.com.bancolombia.binstash.demo.Person;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import tools.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
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
    void testShouldGetKeyset() {

        when(memStash.keySet()).thenReturn(Mono.just(Set.of("pparker")));

        StepVerifier.create(cache.keySet())
                .expectSubscription()
                .expectNext(Set.of("pparker"))
                .expectComplete()
                .verify();

        verify(memStash).keySet();
    }

    @Test
    @DisplayName("Get keys from local")
    void testShouldGetKeys() {

        when(memStash.keys(anyString(), anyInt())).thenReturn(Flux.just("pparker"));

        StepVerifier.create(cache.keys("pp*", 1))
                .expectSubscription()
                .expectNext("pparker")
                .expectComplete()
                .verify();

        verify(memStash).keys("pp*", 1);
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

        StepVerifier.create(cache.evict("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).evict("pparker");
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

    @Test
    @DisplayName("Set save with TTL on local cache only")
    void testSetSaveWithTtlOnlyLocal() {

        when(memStash.setSave(anyString(), anyString(), any(Person.class), anyInt())).thenReturn(Mono.just(p));

        StepVerifier.create(cache.setSave("user:index", "pparker", p, 3600))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).setSave("user:index", "pparker", p, 3600);
        verify(redisStash, times(0)).setSave(anyString(), anyString(), any(Person.class), anyInt());
    }

    @SneakyThrows
    @Test
    @DisplayName("Set save with TTL on local cache and then update distributed")
    void testSetSaveWithTtlLocalAndUpstream() {

        when(memStash.setSave(anyString(), anyString(), any(Person.class), anyInt())).thenReturn(Mono.just(p));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(redisStash.exists(anyString())).thenReturn(Mono.just(false));
        when(redisStash.setSave(anyString(), anyString(), any(Person.class), anyInt())).thenReturn(Mono.just(p));

        StepVerifier.create(cache.setSave("user:index", "pparker", p, 3600))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).setSave("user:index", "pparker", p, 3600);
        verify(redisStash, timeout(1000)).exists("user:index");
        verify(redisStash, timeout(1000)).setSave("user:index", "pparker", p, 3600);
    }

    @Test
    @DisplayName("Set save without TTL on local cache only")
    void testSetSaveOnlyLocal() {

        when(memStash.setSave(anyString(), anyString(), any(Person.class))).thenReturn(Mono.just(p));

        StepVerifier.create(cache.setSave("user:index", "pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).setSave("user:index", "pparker", p);
        verify(redisStash, times(0)).setSave(anyString(), anyString(), any(Person.class));
    }

    @SneakyThrows
    @Test
    @DisplayName("Set save without TTL on local cache and then update distributed")
    void testSetSaveLocalAndUpstream() {

        when(memStash.setSave(anyString(), anyString(), any(Person.class))).thenReturn(Mono.just(p));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(redisStash.exists(anyString())).thenReturn(Mono.just(false));
        when(redisStash.setSave(anyString(), anyString(), any(Person.class))).thenReturn(Mono.just(p));

        StepVerifier.create(cache.setSave("user:index", "pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).setSave("user:index", "pparker", p);
        verify(redisStash, timeout(1000)).exists("user:index");
        verify(redisStash, timeout(1000)).setSave("user:index", "pparker", p);
    }

    @Test
    @DisplayName("Set get all from local cache")
    void testSetGetAllFromLocal() {

        when(memStash.setGetAll(anyString(), any())).thenReturn(Flux.just(p));

        StepVerifier.create(cache.setGetAll("user:index", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).setGetAll(eq("user:index"), any());
        verify(redisStash, times(0)).setGetAll(anyString(), any());
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss local cache on set get all, then fetch from centralized, but no save local")
    void testSetGetAllFromLocalAndUpstreamNotSyncDownstream() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(false);

        when(memStash.setGetAll(anyString(), any())).thenReturn(Flux.empty());
        when(redisStash.setGetAll(anyString(), any())).thenReturn(Flux.just(p));

        StepVerifier.create(cache.setGetAll("user:index", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).setGetAll(eq("user:index"), any());
        verify(memStash, times(0)).setSave(anyString(), anyString(), any(Person.class));
        verify(redisStash).setGetAll(eq("user:index"), any());
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss local cache on set get all, then fetch from centralized, then sync local cache")
    void testSetGetAllFromLocalAndUpstreamAndSyncDownstream() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(true);

        when(memStash.setGetAll(anyString(), any())).thenReturn(Flux.empty());
        when(redisStash.setGetAll(anyString(), any())).thenReturn(Flux.just(p));
        when(memStash.setSave(anyString(), anyString(), any(Person.class))).thenReturn(Mono.just(p));

        StepVerifier.create(cache.setGetAll("user:index", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).setGetAll(eq("user:index"), any());
        verify(memStash, timeout(1000)).setSave(eq("user:index"), eq("user:index"), any(Person.class));
        verify(redisStash).setGetAll(eq("user:index"), any());
    }

    @Test
    @DisplayName("Miss local and centralized caches on set get all")
    void testSetGetAllShouldNotGetFromRedis() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(memStash.setGetAll(anyString(), any())).thenReturn(Flux.empty());
        when(redisStash.setGetAll(anyString(), any())).thenReturn(Flux.empty());

        StepVerifier.create(cache.setGetAll("user:index", Person.class))
                .expectSubscription()
                .expectComplete()
                .verify();

        verify(memStash).setGetAll(eq("user:index"), any());
        verify(redisStash).setGetAll(eq("user:index"), any());
    }

    @Test
    @DisplayName("Set remove key in local cache only")
    void testSetRemove() {

        when(memStash.setRemove(anyString(), anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.setRemove("user:index", "pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).setRemove("user:index", "pparker");
        verify(redisStash, times(0)).setRemove(anyString(), anyString());
    }

    @SneakyThrows
    @Test
    @DisplayName("Set remove key in local cache then @ centralized")
    void testSetRemoveDoubleTier() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(memStash.setRemove(anyString(), anyString())).thenReturn(Mono.just(true));
        when(redisStash.setRemove(anyString(), anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.setRemove("user:index", "pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verifyThenAssertThat()
                .hasNotDiscardedElements()
                .hasNotDroppedElements();

        verify(memStash).setRemove("user:index", "pparker");
        verify(redisStash, timeout(1000)).setRemove("user:index", "pparker");
    }
}
