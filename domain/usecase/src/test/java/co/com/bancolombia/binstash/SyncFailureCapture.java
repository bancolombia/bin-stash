package co.com.bancolombia.binstash;

import reactor.core.publisher.Hooks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Captures, for one test, the sync failures logged by a hybrid use case and the errors Reactor drops.
 */
final class SyncFailureCapture implements AutoCloseable {

    private final Logger logger;
    private final Handler handler;
    private final CountDownLatch logged = new CountDownLatch(1);
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final List<Throwable> dropped = new CopyOnWriteArrayList<>();

    SyncFailureCapture(Class<?> useCase, String key) {
        this.logger = Logger.getLogger(useCase.getName());
        String keyToken = "key " + key + " ";
        this.handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                // only this test's key, so other tests logging concurrently can't interfere
                String message = logRecord.getMessage();
                if (message != null && message.contains(keyToken)) {
                    messages.add(message);
                    logged.countDown();
                }
            }

            @Override
            public void flush() {
                // nothing to flush
            }

            @Override
            public void close() {
                // nothing to close
            }
        };
        logger.addHandler(handler);
        Hooks.onErrorDropped(dropped::add);
    }

    boolean awaitLogged() throws InterruptedException {
        return logged.await(2, TimeUnit.SECONDS);
    }

    List<String> messages() {
        return messages;
    }

    List<Throwable> dropped() {
        return dropped;
    }

    @Override
    public void close() {
        logger.removeHandler(handler);
        Hooks.resetOnErrorDropped();
    }
}
