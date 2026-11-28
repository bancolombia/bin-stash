package co.com.bancolombia.binstash.adapter.redis;

import io.lettuce.core.KeyValue;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisStashSetGetAllBatchTest {

    @Mock
    private RedisReactiveCommands<String, String> redisReactiveCommands;

    @Test
    @DisplayName("setGetAll should read members with batched MGET instead of one GET per key")
    void setGetAllUsesBatchedReads() {
        RedisStash stash = new RedisStash(redisReactiveCommands, 1);
        List<String> keys = IntStream.range(0, 100).mapToObj(i -> "k" + i).toList();
        List<List<String>> askedBatches = new ArrayList<>();

        when(redisReactiveCommands.smembers("idx")).thenReturn(Flux.fromIterable(keys));
        when(redisReactiveCommands.mget(any(String[].class))).thenAnswer(invocation -> {
            String[] asked = mgetKeys(invocation);
            askedBatches.add(List.of(asked));
            return Flux.fromArray(asked).map(key -> KeyValue.just(key, "v-" + key));
        });

        StepVerifier.create(stash.setGetAll("idx"))
                .expectNextCount(100)
                .verifyComplete();

        assertEquals(1, askedBatches.size());
        assertEquals(keys, askedBatches.get(0));
        verify(redisReactiveCommands).smembers("idx");
        verify(redisReactiveCommands, times(1)).mget(any(String[].class));
        verify(redisReactiveCommands, never()).get(anyString());
        verify(redisReactiveCommands, never()).srem(anyString(), any());
    }

    @Test
    @DisplayName("setGetAll should SREM expired members once per batch")
    void setGetAllRemovesExpiredMembersInOneSrem() {
        RedisStash stash = new RedisStash(redisReactiveCommands, 1);
        List<String> keys = List.of("live-a", "dead", "live-b");

        when(redisReactiveCommands.smembers("idx")).thenReturn(Flux.fromIterable(keys));
        when(redisReactiveCommands.mget(any(String[].class))).thenReturn(Flux.just(
                KeyValue.just("live-a", "ok-a"),
                KeyValue.empty("dead"),
                KeyValue.just("live-b", "ok-b")));
        when(redisReactiveCommands.srem(eq("idx"), eq("dead"))).thenReturn(Mono.just(1L));

        StepVerifier.create(stash.setGetAll("idx"))
                .expectNext("ok-a", "ok-b")
                .verifyComplete();

        verify(redisReactiveCommands, times(1)).srem(eq("idx"), eq("dead"));
        verify(redisReactiveCommands, never()).get(anyString());
    }

    @Test
    @DisplayName("setGetAll should split MGET into batches of 500")
    void setGetAllBatchesMget() {
        RedisStash stash = new RedisStash(redisReactiveCommands, 1);
        int total = RedisStash.SET_GET_ALL_BATCH_SIZE + 1;
        List<String> keys = IntStream.range(0, total).mapToObj(i -> "k" + i).toList();
        List<List<String>> askedBatches = new ArrayList<>();

        when(redisReactiveCommands.smembers("idx")).thenReturn(Flux.fromIterable(keys));
        when(redisReactiveCommands.mget(any(String[].class))).thenAnswer(invocation -> {
            String[] asked = mgetKeys(invocation);
            askedBatches.add(List.of(asked));
            return Flux.fromArray(asked).map(key -> KeyValue.just(key, "v-" + key));
        });

        StepVerifier.create(stash.setGetAll("idx"))
                .expectNextCount(total)
                .verifyComplete();

        assertEquals(2, askedBatches.size());
        assertEquals(RedisStash.SET_GET_ALL_BATCH_SIZE, askedBatches.get(0).size());
        assertEquals(1, askedBatches.get(1).size());
        List<String> asked = new ArrayList<>();
        askedBatches.forEach(asked::addAll);
        assertEquals(keys, asked);
        verify(redisReactiveCommands, never()).get(anyString());
    }

    private static String[] mgetKeys(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        if (args.length == 1 && args[0] instanceof String[] keys) {
            return keys;
        }
        return Arrays.copyOf(args, args.length, String[].class);
    }
}
