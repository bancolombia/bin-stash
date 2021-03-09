package co.com.bancolombia.binstash;

import co.com.bancolombia.binstash.model.api.HashStash;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SingleTierMapCacheUseCaseTest {

    private SingleTierMapCacheUseCase cache;

    private Map<String, String> demoMap;

    @Mock
    private HashStash mockedStash;

    @BeforeEach
    public void before() {
        cache = new SingleTierMapCacheUseCase(mockedStash);
        demoMap = new HashMap<>();
        demoMap.put("name", "Peter");
        demoMap.put("lastName", "Parker");
    }

    @Test
    @DisplayName("Create cache")
    public void testCreate() {
        assert cache != null;
    }

    @Test
    @DisplayName("save map cache")
    public void testSave() {

        when(mockedStash.hSave(anyString(), any(Map.class))).thenReturn(Mono.just(demoMap));

        StepVerifier.create(cache.saveMap("pparker", demoMap))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        verify(mockedStash).hSave(eq("pparker"), eq(demoMap));
    }

    @Test
    @DisplayName("save map prop in cache")
    public void testSaveProp() {

        when(mockedStash.hSave(anyString(), anyString(), anyString())).thenReturn(Mono.just("NY"));

        StepVerifier.create(cache.saveMap("pparker", "city", "NY"))
                .expectSubscription()
                .expectNext("NY")
                .expectComplete()
                .verify();

        verify(mockedStash).hSave(eq("pparker"), eq("city"), eq("NY"));
    }

    @Test
    @DisplayName("Get map from cache")
    public void testGet() {

        when(mockedStash.hGetAll(anyString())).thenReturn(Mono.just(demoMap));

        StepVerifier.create(cache.getMap("pparker"))
                .expectSubscription()
                .expectNext(demoMap)
                .expectComplete()
                .verify();

        verify(mockedStash).hGetAll(eq("pparker"));
    }

    @Test
    @DisplayName("Get map field from cache")
    public void testGetField() {

        when(mockedStash.hGet(anyString(), anyString())).thenReturn(Mono.just("Peter"));

        StepVerifier.create(cache.getMap("pparker", "name"))
                .expectSubscription()
                .expectNext("Peter")
                .expectComplete()
                .verify();

        verify(mockedStash).hGet(eq("pparker"), eq("name"));
    }

    @Test
    @DisplayName("Get map field from cache II")
    public void testGetField2() {

        when(mockedStash.hGet(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(cache.getMap("pparker", "name"))
                .expectSubscription()
                .expectComplete()
                .verify();

        verify(mockedStash).hGet(eq("pparker"), eq("name"));
    }

    @Test
    @DisplayName("Check map exists on cache")
    public void testExist() {

        when(mockedStash.hGetAll(anyString())).thenReturn(Mono.just(demoMap));

        StepVerifier.create(cache.existsMap("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).hGetAll(eq("pparker"));
    }

    @Test
    @DisplayName("Check map doesnt exists on cache")
    public void testNotExist() {

        when(mockedStash.hGetAll(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(cache.existsMap("pparker"))
                .expectSubscription()
                .expectNext(false)
                .expectComplete()
                .verify();

        verify(mockedStash).hGetAll(eq("pparker"));
    }

    @Test
    @DisplayName("Check field exists on map cache")
    public void testFieldExist() {

        when(mockedStash.hGet(anyString(), anyString())).thenReturn(Mono.just("Peter"));

        StepVerifier.create(cache.existsMap("pparker", "name"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).hGet(eq("pparker"), eq("name"));
    }

    @Test
    @DisplayName("Verify field doesnt exists on map cache")
    public void testFieldNotExist() {

        when(mockedStash.hGet(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(cache.existsMap("pparker", "name"))
                .expectSubscription()
                .expectNext(false)
                .expectComplete()
                .verify();

        verify(mockedStash).hGet(eq("pparker"), eq("name"));
    }

    @Test
    @DisplayName("evict map in cache")
    public void testEvict() {

        when(mockedStash.hDelete(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evictMap("pparker"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).hDelete(eq("pparker"));
    }

    @Test
    @DisplayName("evict field in map cache")
    public void testEvictField() {

        when(mockedStash.hDelete(anyString(), anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(cache.evictMap("pparker", "name"))
                .expectSubscription()
                .expectNext(true)
                .expectComplete()
                .verify();

        verify(mockedStash).hDelete(eq("pparker"), eq("name"));
    }


}
