package co.com.bancolombia.binstash;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * Background sync between the local and centralized tiers of the hybrid cache.
 * Failures are logged instead of being dropped; neither the key nor the exception message is logged at WARNING,
 * because both may contain user data.
 */
@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class HybridSync {

    private static final Scheduler ELASTIC_SCHEDULER = Schedulers.boundedElastic();

    /**
     * Runs the centralized operation in the background if the upstream rule allows it. Call it only after the local
     * operation completed without error.
     */
    static void upstream(RuleEvaluatorUseCase ruleEvaluatorUseCase, String key,
                         Supplier<Mono<?>> centralizedOperation) {
        Mono.fromCallable(() -> ruleEvaluatorUseCase.evalForUpstreamSync(key))
                .filter(shouldSync -> shouldSync)
                .flatMap(shouldSync -> Mono.defer(centralizedOperation).subscribeOn(ELASTIC_SCHEDULER))
                .subscribe(null, error -> logFailure("upstream", key, error));
    }

    /**
     * Runs the local operation if the downstream rule allows it.
     */
    static void downstream(RuleEvaluatorUseCase ruleEvaluatorUseCase, String key, Supplier<Mono<?>> localOperation) {
        Mono.fromCallable(() -> ruleEvaluatorUseCase.evalForDownstreamSync(key))
                .filter(shouldSync -> shouldSync)
                .flatMap(shouldSync -> Mono.defer(localOperation))
                .subscribe(null, error -> logFailure("downstream", key, error));
    }

    private static void logFailure(String direction, String key, Throwable error) {
        // the key is only logged at FINE, right before the warning, to correlate them when debugging
        log.fine(() -> String.format("Hybrid cache %s sync failed for key %s", direction, key));
        log.warning(() -> String.format("Hybrid cache %s sync failed: %s", direction, error.getClass().getName()));
    }
}
