package co.com.bancolombia.binstash.adapter.redis;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;

public class RedisStashFactory {

    private RedisStashFactory() {
        // private constructor
    }

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

        return new RedisStash(primaryAndReplicaConnection.reactive(), properties.getExpireAfter());
    }

    public static RedisStash redisStashSingle(RedisProperties properties) {

        RedisClient redisClient =
                RedisClient.create(properties.getPrimaryURI());

        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        return new RedisStash(redisReactiveCommands, properties.getExpireAfter());
    }
}
