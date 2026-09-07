package co.com.bancolombia.binstash.adapter.redis;

import io.lettuce.core.RedisURI;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
@Setter
public class RedisProperties {
    private String username;
    private String password;
    private String host;
    private String hostReplicas;
    private int port;
    private int database;
    private boolean useSsl;
    private int expireTime;

    public boolean isMasterReplica() {
        return this.hostReplicas != null && !this.hostReplicas.isEmpty();
    }

    // Leaved for compatibility because is public method
    public boolean isRbac() {
        return hasValue(this.username) && hasValue(this.password);
    }

    public RedisURI getPrimaryURI() {
        return getURI(this.host);
    }

    public List<RedisURI> getAllURIs() {
        if (isMasterReplica()) {
            List<RedisURI> allNodes = Arrays.stream(this.hostReplicas.split(","))
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .map(this::getURI)
                    .collect(Collectors.toList());
            allNodes.add(0, getPrimaryURI());
            return allNodes;
        } else {
            return Collections.singletonList(getPrimaryURI());
        }
    }

    private RedisURI getURI(String host) {
        String finalHost = host;
        int finalPort = this.port;

        if (host.contains(":")) {
            String[] parts = host.split(":");
            finalHost = parts[0];
            finalPort = Integer.parseInt(parts[1]);
        }

        var builder = RedisURI.builder()
                .withHost(finalHost)
                .withPort(finalPort);

        if (hasValue(this.password)) {
            if (hasValue(this.username)) {
                builder = builder.withAuthentication(this.username, this.password.toCharArray());
            } else {
                builder = builder.withPassword(this.password.toCharArray());
            }
        }

        if (this.database > 0) {
            builder = builder.withDatabase(this.database);
        }
        builder = builder.withSsl(this.useSsl);
        return builder.build();
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
