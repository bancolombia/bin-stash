package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.MapCache;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DoubleTierMapCacheUseCaseTest {

    private DoubleTierMapCacheUseCase cache;

    @Mock
    private MapCache localCache;
    @Mock
    private MapCache distributedCache;
    @Mock
    private RuleEvaluatorUseCase ruleEvaluatorUseCase;

    private Map<String, String> demoMap;

    @BeforeEach
    public void before() {
        cache = new DoubleTierMapCacheUseCase(localCache, distributedCache, ruleEvaluatorUseCase);
        demoMap = new HashMap<>();
        demoMap.put("name", "Peter");
        demoMap.put("lastName", "Parker");
    }

    @Test
    @DisplayName("Create cache")
    public void testCreate() {
        assert cache != null;
    }

    @SneakyThrows
    @Test
    @DisplayName("save local map cache, sync upstream")
    public void testSave() {

        when(localCache.saveMap(anyString(), any(Map.class))).thenReturn(Mono.just(demoMap));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(distributedCache.existsMap(anyString())).thenReturn(Mono.just(false));
        when(distributedCache.saveMap(anyString(), any(Map.class))).thenReturn(Mono.just(demoMap));

        StepVerifier.create(cache.saveMap("pparker", demoMap))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(localCache).saveMap(eq("pparker"), eq(demoMap));
        verify(distributedCache).saveMap(eq("pparker"), eq(demoMap));
    }

    @SneakyThrows
    @Test
    @DisplayName("save local map cache, try sync upstream, key exists")
    public void testSave2() {

        when(localCache.saveMap(anyString(), any(Map.class))).thenReturn(Mono.just(demoMap));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(distributedCache.existsMap(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.saveMap("pparker", demoMap))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();
        Thread.sleep(300);
        verify(localCache).saveMap(eq("pparker"), eq(demoMap));
        verify(distributedCache, times(0)).saveMap(eq("pparker"), eq(demoMap));
    }

    @SneakyThrows
    @Test
    @DisplayName("save local map cache, dont sync upstream")
    public void testSave3() {

        when(localCache.saveMap(anyString(), any(Map.class))).thenReturn(Mono.just(demoMap));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(false);

        StepVerifier.create(cache.saveMap("pparker", demoMap))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        Thread.sleep(300);
        verify(localCache).saveMap(eq("pparker"), eq(demoMap));
        verify(distributedCache, times(0)).saveMap(eq("pparker"), eq(demoMap));
    }


    @SneakyThrows
    @Test
    @DisplayName("save map prop in local cache, sync upstream")
    public void testSaveProp() {
        when(localCache.saveMap(anyString(), anyString(), anyString())).thenReturn(Mono.just("NY"));
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(distributedCache.existsMap(anyString(), anyString())).thenReturn(Mono.just(false));
        when(distributedCache.saveMap(anyString(), anyString(), anyString())).thenReturn(Mono.just("NY"));

        StepVerifier.create(cache.saveMap("pparker", "city", "NY"))
                .expectSubscription()
                .expectNext("NY")
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(localCache).saveMap(eq("pparker"), eq("city"), eq("NY"));
        verify(distributedCache).saveMap(eq("pparker"), eq("city"), eq("NY"));
    }


    @SneakyThrows
    @Test
    @DisplayName("Get map from local cache only")
    public void testGet() {

        when(localCache.getMap(anyString())).thenReturn(Mono.just(demoMap));

        StepVerifier.create(cache.getMap("pparker"))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(distributedCache, times(0)).getMap(eq("pparker"));
    }

    @SneakyThrows
    @Test
    @DisplayName("Get map field from local cache only")
    public void testGetField() {

        when(localCache.getMap(anyString(), anyString())).thenReturn(Mono.just("NY"));

        StepVerifier.create(cache.getMap("pparker", "city"))
                .expectSubscription()
                .expectNext("NY")
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(distributedCache, times(0)).getMap(eq("pparker"));
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss map field from local cache, sync downstream")
    public void testGetField2() {

        when(localCache.getMap(anyString(), anyString())).thenReturn(Mono.empty());
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(distributedCache.getMap(anyString(), anyString())).thenReturn(Mono.just("NY"));
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(true);
        when(localCache.saveMap(anyString(), anyString(), anyString())).thenReturn(Mono.just("NY"));

        StepVerifier.create(cache.getMap("pparker", "city"))
                .expectSubscription()
                .expectNext("NY")
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(distributedCache).getMap(eq("pparker"), eq("city"));
        verify(localCache).saveMap(eq("pparker"), eq("city"), eq("NY"));
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss map field from local cache and miss from distributed")
    public void testGetField3() {

        when(localCache.getMap(anyString(), anyString())).thenReturn(Mono.empty());
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(distributedCache.getMap(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(cache.getMap("pparker", "city"))
                .expectSubscription()
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(distributedCache).getMap(eq("pparker"), eq("city"));
        verify(localCache, times(0)).saveMap(eq("pparker"), eq("city"), eq("NY"));
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss map from local cache, fetch from distributed, sync local")
    public void testGet2() {

        when(localCache.getMap(anyString())).thenReturn(Mono.empty());
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(true);
        when(distributedCache.getMap(anyString())).thenReturn(Mono.just(demoMap));
        when(localCache.saveMap(anyString(), any(Map.class))).thenReturn(Mono.just(demoMap));

        StepVerifier.create(cache.getMap("pparker"))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(distributedCache).getMap(eq("pparker"));
        verify(localCache).saveMap(eq("pparker"), any(Map.class));
    }

    @SneakyThrows
    @Test
    @DisplayName("Miss map from local cache, fetch from distributed, dont sync local")
    public void testGet3() {

        when(localCache.getMap(anyString())).thenReturn(Mono.empty());
        when(ruleEvaluatorUseCase.evalForUpstreamSync(anyString())).thenReturn(true);
        when(ruleEvaluatorUseCase.evalForDownstreamSync(anyString())).thenReturn(false);
        when(distributedCache.getMap(anyString())).thenReturn(Mono.just(demoMap));

        StepVerifier.create(cache.getMap("pparker"))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        Thread.sleep(300);

        verify(distributedCache).getMap(eq("pparker"));
        verify(localCache, times(0)).saveMap(eq("pparker"), any(Map.class));
    }

    @Test
    @DisplayName("Check map exists on local cache")
    public void testExist() {

        when(localCache.existsMap(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.existsMap("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(localCache).existsMap(eq("pparker"));
    }

    @Test
    @DisplayName("Check field exists on map cache")
    public void testFieldExist() {

        when(localCache.existsMap(anyString(), anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.existsMap("pparker", "name"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(localCache).existsMap(eq("pparker"), eq("name"));
    }

    @Test
    @DisplayName("evict map in cache")
    public void testEvict() {

        when(localCache.evictMap(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evictMap("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(localCache).evictMap(eq("pparker"));
    }

    @Test
    @DisplayName("evict field in map cache")
    public void testEvictField() {

        when(localCache.evictMap(anyString(), anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evictMap("pparker", "name"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(localCache).evictMap(eq("pparker"), eq("name"));
    }
}
