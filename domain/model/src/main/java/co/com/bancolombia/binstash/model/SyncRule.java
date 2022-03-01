package co.com.bancolombia.binstash.model;

/**
 * Synchronization rule to be evaluated by the double tier cache in order to decide if a key/value
 * should be synchronized upstream or downstream.
 */
@FunctionalInterface
public interface SyncRule {

    /**
     * Logic to decide if a key (keyArg) should be synchronized <pre>SyncType.UPSTREAM</pre> or
     * <pre>SyncType.DOWNSTREAM</pre>, being the local cache always thw downstream and the centralized
     * the downstream.
     *
     * @param keyArg the key to be evaluated
     * @param syncType the sync to be evaluated. It is either <pre>SyncType.UPSTREAM</pre> or
     *      * <pre>SyncType.DOWNSTREAM</pre>
     * @return true if the key should be propagated in the defined <pre>SyncType</pre> direction, false otherwise.
     */
    boolean apply(String keyArg, SyncType syncType);
}
