package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.demo.Address;
import co.com.bancolombia.binstash.demo.Person;
import co.com.bancolombia.binstash.model.api.ObjectCache;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DoubleTierObjectCacheUseCaseTest {

    private DoubleTierObjectCacheUseCase<Person> cache;

    @Mock
    private RuleEvaluatorUseCase ruleEvaluatorUseCase;

    @Mock
    private ObjectCache<Person> memStash;

    @Mock
    private ObjectCache<Person> redisStash;

    private Person p;

    @BeforeEach
    public void before() {
        p = new Person();
        p.setName("Peter Parker");
        p.setAddress(new Address("some-street", "NY"));
        cache = new DoubleTierObjectCacheUseCase<>(memStash, redisStash, ruleEvaluatorUseCase);
    }

    @Test
    @DisplayName("Create cache instance")
    public void testCreate() {
        assert cache != null;
    }

    @Test
    @DisplayName("Save on local cache only")
    public void testSaveOnlyLocal() {

        when(memStash.save(anyString(), any(Person.class))).thenReturn(Mono.just(p));

        StepVerifier.create(cache.save("pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).save(eq("pparker"), eq(p));
        verify(redisStash, times(0)).save(eq("pparker"), eq(p));
    }

    @SneakyThrows
    @Test
    @DisplayName("Save on local cache and then update distrubuted")
    public void testSaveLocalAndUpstream() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(memStash.save(anyString(), any(Person.class))).thenReturn(Mono.just(p));
        when(redisStash.exists(anyString())).thenReturn(Mono.just(false));
        when(redisStash.save(anyString(), any(Person.class))).thenReturn(Mono.just(p));

        StepVerifier.create(cache.save("pparker", p))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(memStash).save(eq("pparker"), eq(p));
        verify(redisStash).exists(eq("pparker"));
        verify(redisStash).save(eq("pparker"), eq(p));
    }

    @Test
    @DisplayName("Get from local cache")
    public void testGetFromLocal() {

        when(memStash.get(anyString(), any())).thenReturn(Mono.just(p));

        StepVerifier.create(cache.get("pparker", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        verify(memStash).get(eq("pparker"), any());
        verify(memStash, times(0)).save(eq("pparker"), eq(p));
        verify(redisStash, times(0)).get(eq("pparker"), any());
        verify(redisStash, times(0)).save(eq("pparker"), eq(p));
    }

    @Test
    @DisplayName("Check element exists on local cache")
    public void testExist() {

        when(memStash.exists(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.exists("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(memStash).exists(eq("pparker"));
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss local cache, then fetch from distributed, but no save local")
    public void testGetFromLocalAndUpstreamNotSyncDownstream() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(false);

        when(memStash.get(anyString(), any())).thenReturn(Mono.empty());
        when(redisStash.get(anyString(), any())).thenReturn(Mono.just(p));

        StepVerifier.create(cache.get("pparker", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(memStash).get(eq("pparker"), any());
        verify(memStash, times(0)).save(eq("pparker"), eq(p));
        verify(redisStash).get(eq("pparker"), any());
        verify(redisStash, times(0)).save(eq("pparker"), eq(p));
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss local cache, then fetch from distributed, then sync local cache")
    public void testGetFromLocalAndUpstreamAndSyncDownstream() {

        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(true);

        when(memStash.get(anyString(), any())).thenReturn(Mono.empty());
        when(redisStash.get(anyString(), any())).thenReturn(Mono.just(p));
        when(memStash.save(anyString(), any())).thenReturn(Mono.just(p));

        StepVerifier.create(cache.get("pparker", Person.class))
                .expectSubscription()
                .expectNext(p)
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(memStash).get(eq("pparker"), any());
        verify(memStash).save(eq("pparker"), eq(p));
        verify(redisStash).get(eq("pparker"), any());
        verify(redisStash, times(0)).save(eq("pparker"), eq(p));
    }

    @Test
    @DisplayName("Miss local and distributed caches")
    public void testShouldNotGetFromRedis() {

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
    @DisplayName("evict key in local cache only")
    public void testEvict() {
        when(memStash.evict(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evict("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify(Duration.ofMillis(300));

        verify(memStash).evict(eq("pparker"));
        verify(redisStash, times(0)).evict(eq("pparker"));
    }

    @SneakyThrows
    @Test
    @DisplayName("evict key in local cache then @ distributed")
    public void testEvictDobleTier() {
        when(memStash.evict(anyString())).thenReturn(Mono.just(true));
        when(redisStash.evict(anyString())).thenReturn(Mono.just(true));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);

        StepVerifier.create(cache.evict("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(memStash).evict(eq("pparker"));
        verify(redisStash).evict(eq("pparker"));
    }

    @Test
    @DisplayName("evict all keys")
    public void testEvictAll() {

        when(memStash.evictAll()).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evictAll())
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(memStash).evictAll();
    }
}
