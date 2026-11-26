package co.com.bancolombia.binstash;

import reactor.core.publisher.Hooks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Captures, for one test, the sync failure warning logged by {@link HybridSync} and the errors Reactor drops.
 * The warning has no key, so it is matched to the test by the FINE record with the key that precedes it on the
 * same thread.
 */
final class SyncFailureCapture implements AutoCloseable {

    private final Logger logger = Logger.getLogger(HybridSync.class.getName());
    private final Level previousLevel = logger.getLevel();
    private final Handler handler;
    private final CountDownLatch logged = new CountDownLatch(1);
    private final List<String> warnings = new CopyOnWriteArrayList<>();
    private final List<Throwable> dropped = new CopyOnWriteArrayList<>();

    SyncFailureCapture(String key) {
        String keyToken = "key " + key;
        this.handler = new Handler() {
            private final ThreadLocal<Boolean> keyLogged = ThreadLocal.withInitial(() -> false);

            @Override
            public void publish(LogRecord logRecord) {
                String message = logRecord.getMessage();
                if (message == null) {
                    return;
                }
                if (logRecord.getLevel() == Level.FINE) {
                    keyLogged.set(message.endsWith(keyToken));
                } else if (logRecord.getLevel() == Level.WARNING && Boolean.TRUE.equals(keyLogged.get())) {
                    keyLogged.set(false);
                    warnings.add(message);
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
        handler.setLevel(Level.ALL);
        logger.setLevel(Level.FINE);
        logger.addHandler(handler);
        Hooks.onErrorDropped(dropped::add);
    }

    boolean awaitLogged() throws InterruptedException {
        return logged.await(2, TimeUnit.SECONDS);
    }

    List<String> warnings() {
        return warnings;
    }

    List<Throwable> dropped() {
        return dropped;
    }

    @Override
    public void close() {
        logger.removeHandler(handler);
        logger.setLevel(previousLevel);
        Hooks.resetOnErrorDropped();
    }
}
