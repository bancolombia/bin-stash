package co.com.bancolombia.binstash.model;

@FunctionalInterface
public interface SyncRule {
    boolean apply(String keyArg, SyncType syncType);
}
