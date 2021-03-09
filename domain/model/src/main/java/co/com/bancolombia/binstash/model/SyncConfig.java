package co.com.bancolombia.binstash.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SyncConfig {

    private final boolean syncUpstream;
    private final boolean syncDownstream;

}
