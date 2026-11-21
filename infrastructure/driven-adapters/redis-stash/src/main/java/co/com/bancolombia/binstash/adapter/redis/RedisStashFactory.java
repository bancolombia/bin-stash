package co.com.bancolombia.binstash.adapter.redis;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisStashFactory {

    public static RedisStash redisStash(RedisProperties properties) {
        if (properties.isMasterReplica()) {
            return redisStashMasterReplica(properties);
        } else {
            return redisStashSingle(properties);
        }
    }

    public static RedisStash redisStashMasterReplica(RedisProperties properties) {

        RedisClient redisClient = RedisClient.create(
                properties.getPrimaryURI()
        );

        StatefulRedisMasterReplicaConnection<String, String> primaryAndReplicaConnection = MasterReplica.connect(
                redisClient,
                StringCodec.UTF8,
                properties.getAllURIs()
        );

        primaryAndReplicaConnection.setReadFrom(ReadFrom.REPLICA);

        return new RedisStash(
                primaryAndReplicaConnection.commands(RedisReactiveCommands.factory()),
                properties.getExpireTime()
        );
    }

    public static RedisStash redisStashSingle(RedisProperties properties) {

        RedisClient redisClient = RedisClient.create(properties.getPrimaryURI());

        RedisReactiveCommands<String, String> redisReactiveCommands =
                redisClient.connect()
                .commands(RedisReactiveCommands.factory());

        return new RedisStash(redisReactiveCommands, properties.getExpireTime());
    }
}
