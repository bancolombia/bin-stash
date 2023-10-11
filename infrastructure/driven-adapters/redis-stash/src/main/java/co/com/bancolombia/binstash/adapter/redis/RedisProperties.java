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

    public boolean isRbac() {
        return (this.username != null && !this.username.isBlank()) &&
                (this.password != null && !this.password.isBlank());
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
        var builder = RedisURI.builder()
                .withHost(host)
                .withPort(this.port);

        if (isRbac()) {
            builder = builder.withAuthentication(this.username, this.password.toCharArray());
        }

        if (this.password != null && !this.password.isBlank()){
            builder = builder.withPassword(this.password.toCharArray());
        }

        if (this.database > 0) {
            builder = builder.withDatabase(this.database);
        }
        builder = builder.withSsl(this.useSsl);
        return builder.build();
    }
}
