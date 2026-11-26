package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.demo.Address;
import co.com.bancolombia.binstash.demo.Person;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import co.com.bancolombia.binstash.model.api.StringStash;
import tools.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    @DisplayName("Set get all from centralized returns every member on repeated calls and is not copied locally")
    void testSetGetAllFromCentralizedIsNotCopiedDownstream() {
        Person p2 = new Person();
        p2.setName("Mary Jane");
        Person p3 = new Person();
        p3.setName("Harry Osborn");

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);

        when(memStash.setGetAll(anyString(), any())).thenReturn(Flux.empty());
        when(redisStash.setGetAll(anyString(), any())).thenReturn(Flux.just(p, p2, p3));

        for (int call = 0; call < 2; call++) {
            StepVerifier.create(cache.setGetAll("user:index", Person.class))
                    .expectSubscription()
                    .expectNext(p, p2, p3)
                    .verifyComplete();
        }

        // the centralized set only returns values, not member keys, so it can't be rebuilt locally
        verify(memStash, after(200).never()).setSave(anyString(), anyString(), any(Person.class));
        verify(redisStash, times(2)).setGetAll(eq("user:index"), any());
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

    @Test
    @DisplayName("Unreadable local value falls back to centralized cache")
    void testUnreadableLocalValueFallsBackToCentralized() {
        StringStash localStash = mock(StringStash.class);
        when(localStash.get("pparker")).thenReturn(Mono.just("not-json"));
        ObjectCache<Person> localCache =
                new SingleTierObjectCacheUseCase<>(localStash, new SerializatorHelper<>(new ObjectMapper()));
        DoubleTierObjectCacheUseCase<Person> hybrid =
                new DoubleTierObjectCacheUseCase<>(localCache, redisStash, ruleEvaluatorUseCase);

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(false);
        when(redisStash.get(eq("pparker"), any())).thenReturn(Mono.just(p));

        StepVerifier.create(hybrid.get("pparker", Person.class))
                .expectSubscription()
                .expectNext(p)
                .verifyComplete();

        verify(redisStash).get(eq("pparker"), any());
    }

    @Test
    @DisplayName("Failed local save is not synced upstream")
    void testFailedLocalSaveIsNotSyncedUpstream() {
        when(memStash.save(anyString(), any(Person.class), anyInt()))
                .thenReturn(Mono.error(new IllegalStateException("local failure")));
        lenient().when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);

        StepVerifier.create(cache.save("pparker", p, 60))
                .expectError(IllegalStateException.class)
                .verify();

        // the sync rule is evaluated synchronously when the sync is triggered, so this check is deterministic
        verify(ruleEvaluatorUseCase, never()).evalForUpstreamSync(anyString());
        verify(redisStash, after(200).never()).exists(anyString());
        verify(redisStash, never()).save(anyString(), any(Person.class), anyInt());
    }

    @Test
    @DisplayName("Centralized sync error is logged without its message instead of being dropped")
    void testCentralizedSyncErrorIsLogged() throws InterruptedException {
        when(memStash.save(anyString(), any(Person.class))).thenReturn(Mono.just(p));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(redisStash.exists(anyString())).thenReturn(Mono.error(new IllegalStateException("SECRET-DETAIL")));

        try (SyncFailureCapture capture = new SyncFailureCapture("upsync-err")) {
            StepVerifier.create(cache.save("upsync-err", p))
                    .expectNext(p)
                    .verifyComplete();

            assertTrue(capture.awaitLogged());
            String message = capture.warnings().get(0);
            assertTrue(message.contains("upstream"));
            assertTrue(message.contains(IllegalStateException.class.getName()));
            assertFalse(message.contains("SECRET-DETAIL"));
            // the key may contain user data, so it is only logged at FINE
            assertFalse(message.contains("upsync-err"));
            assertTrue(capture.dropped().isEmpty());
        }
    }

    @Test
    @DisplayName("Downstream sync error is logged instead of being dropped")
    void testDownstreamSyncErrorIsLogged() throws InterruptedException {
        when(memStash.get(anyString(), any(Class.class))).thenReturn(Mono.empty());
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(true);
        when(redisStash.get(anyString(), any(Class.class))).thenReturn(Mono.just(p));
        when(memStash.save(anyString(), any(Person.class)))
                .thenReturn(Mono.error(new IllegalStateException("local failure")));

        try (SyncFailureCapture capture = new SyncFailureCapture("downsync-err")) {
            StepVerifier.create(cache.get("downsync-err", Person.class))
                    .expectNext(p)
                    .verifyComplete();

            assertTrue(capture.awaitLogged());
            String message = capture.warnings().get(0);
            assertTrue(message.contains("downstream"));
            assertFalse(message.contains("downsync-err"));
            assertTrue(capture.dropped().isEmpty());
        }
    }

    @Test
    @DisplayName("Failed local set save is not synced upstream")
    void testFailedLocalSetSaveIsNotSyncedUpstream() {
        when(memStash.setSave(anyString(), anyString(), any(Person.class), anyInt()))
                .thenReturn(Mono.error(new IllegalStateException("local failure")));

        StepVerifier.create(cache.setSave("user:index", "pparker", p, 60))
                .expectError(IllegalStateException.class)
                .verify();

        verify(ruleEvaluatorUseCase, never()).evalForUpstreamSync(anyString());
        verify(redisStash, after(200).never()).exists(anyString());
        verify(redisStash, never()).setSave(anyString(), anyString(), any(Person.class), anyInt());
    }

    @Test
    @DisplayName("Failed local set remove is not synced upstream")
    void testFailedLocalSetRemoveIsNotSyncedUpstream() {
        when(memStash.setRemove(anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("local failure")));

        StepVerifier.create(cache.setRemove("user:index", "pparker"))
                .expectError(IllegalStateException.class)
                .verify();

        verify(ruleEvaluatorUseCase, never()).evalForUpstreamSync(anyString());
        verify(redisStash, after(200).never()).setRemove(anyString(), anyString());
    }

    @Test
    @DisplayName("Failed local evict is not synced upstream")
    void testFailedLocalEvictIsNotSyncedUpstream() {
        when(memStash.evict(anyString())).thenReturn(Mono.error(new IllegalStateException("local failure")));

        StepVerifier.create(cache.evict("pparker"))
                .expectError(IllegalStateException.class)
                .verify();

        verify(ruleEvaluatorUseCase, never()).evalForUpstreamSync(anyString());
        verify(redisStash, after(200).never()).evict(anyString());
    }
}
