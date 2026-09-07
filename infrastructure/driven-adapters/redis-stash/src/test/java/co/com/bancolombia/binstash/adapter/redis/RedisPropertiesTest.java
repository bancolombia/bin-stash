package co.com.bancolombia.binstash.adapter.redis;

import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedisPropertiesTest {

    private RedisProperties buildBase() {
        RedisProperties props = new RedisProperties();
        props.setHost("localhost");
        props.setPort(6379);
        return props;
    }

    // ---------- isMasterReplica ----------

    @Test
    void shouldNotBeMasterReplicaWhenHostReplicasIsNull() {
        RedisProperties props = buildBase();
        assertFalse(props.isMasterReplica());
    }

    @Test
    void shouldNotBeMasterReplicaWhenHostReplicasIsEmpty() {
        RedisProperties props = buildBase();
        props.setHostReplicas("");
        assertFalse(props.isMasterReplica());
    }

    @Test
    void shouldBeMasterReplicaWhenHostReplicasHasValue() {
        RedisProperties props = buildBase();
        props.setHostReplicas("replica1:6380");
        assertTrue(props.isMasterReplica());
    }

    // ---------- isRbac ----------

    @Test
    void shouldBeRbacWhenUsernameAndPasswordPresent() {
        RedisProperties props = buildBase();
        props.setUsername("user");
        props.setPassword("secret");
        assertTrue(props.isRbac());
    }

    @Test
    void shouldNotBeRbacWhenUsernameMissing() {
        RedisProperties props = buildBase();
        props.setPassword("secret");
        assertFalse(props.isRbac());
    }

    @Test
    void shouldNotBeRbacWhenPasswordMissing() {
        RedisProperties props = buildBase();
        props.setUsername("user");
        assertFalse(props.isRbac());
    }

    @Test
    void shouldNotBeRbacWhenBothBlank() {
        RedisProperties props = buildBase();
        props.setUsername("   ");
        props.setPassword("   ");
        assertFalse(props.isRbac());
    }

    // ---------- getPrimaryURI ----------

    @Test
    void shouldBuildPrimaryUriWithHostAndPort() {
        RedisProperties props = buildBase();

        RedisURI uri = props.getPrimaryURI();

        assertEquals("localhost", uri.getHost());
        assertEquals(6379, uri.getPort());
        assertFalse(uri.isSsl());
    }

    @Test
    void shouldUsePortFromHostWhenHostContainsColon() {
        RedisProperties props = buildBase();
        props.setHost("myhost:7000");

        RedisURI uri = props.getPrimaryURI();

        assertEquals("myhost", uri.getHost());
        assertEquals(7000, uri.getPort());
    }

    @Test
    void shouldThrowWhenPortInHostIsNotNumeric() {
        RedisProperties props = buildBase();
        props.setHost("myhost:notaport");

        assertThrows(NumberFormatException.class, props::getPrimaryURI);
    }

    // ---------- authentication on URI ----------

    @Test
    void shouldApplyPasswordOnlyAuthenticationWhenUsernameMissing() {
        RedisProperties props = buildBase();
        props.setPassword("secret");

        RedisURI uri = props.getPrimaryURI();

        StepVerifier.create(uri.getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> {
                    assertNull(credentials.getUsername());
                    assertArrayEquals("secret".toCharArray(), credentials.getPassword());
                })
                .verifyComplete();
    }

    @Test
    void shouldApplyFullAuthenticationWhenUsernameAndPasswordPresent() {
        RedisProperties props = buildBase();
        props.setUsername("user");
        props.setPassword("secret");

        RedisURI uri = props.getPrimaryURI();

        StepVerifier.create(uri.getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> {
                    assertEquals("user", credentials.getUsername());
                    assertArrayEquals("secret".toCharArray(), credentials.getPassword());
                })
                .verifyComplete();
    }

    @Test
    void shouldNotApplyAuthenticationWhenPasswordMissing() {
        RedisProperties props = buildBase();
        props.setUsername("user");

        RedisURI uri = props.getPrimaryURI();

        StepVerifier.create(uri.getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> assertNull(credentials.getPassword()))
                .verifyComplete();
    }

    // ---------- database and ssl ----------

    @Test
    void shouldApplyDatabaseWhenGreaterThanZero() {
        RedisProperties props = buildBase();
        props.setDatabase(3);

        RedisURI uri = props.getPrimaryURI();

        assertEquals(3, uri.getDatabase());
    }

    @Test
    void shouldUseDefaultDatabaseWhenZeroOrNegative() {
        RedisProperties props = buildBase();
        props.setDatabase(0);

        RedisURI uri = props.getPrimaryURI();

        assertEquals(0, uri.getDatabase());
    }

    @Test
    void shouldEnableSslWhenUseSslIsTrue() {
        RedisProperties props = buildBase();
        props.setUseSsl(true);

        RedisURI uri = props.getPrimaryURI();

        assertTrue(uri.isSsl());
    }

    // ---------- getAllURIs ----------

    @Test
    void shouldReturnOnlyPrimaryUriWhenNotMasterReplica() {
        RedisProperties props = buildBase();

        List<RedisURI> uris = props.getAllURIs();

        assertEquals(1, uris.size());
        assertEquals("localhost", uris.get(0).getHost());
        assertEquals(6379, uris.get(0).getPort());
    }

    @Test
    void shouldReturnPrimaryFirstFollowedByReplicasWhenMasterReplica() {
        RedisProperties props = buildBase();
        props.setHostReplicas("replica1:6380, replica2:6381");

        List<RedisURI> uris = props.getAllURIs();

        assertEquals(3, uris.size());
        // El primario siempre va primero
        assertEquals("localhost", uris.get(0).getHost());
        assertEquals(6379, uris.get(0).getPort());
        // Réplicas en orden, con espacios recortados (trim)
        assertEquals("replica1", uris.get(1).getHost());
        assertEquals(6380, uris.get(1).getPort());
        assertEquals("replica2", uris.get(2).getHost());
        assertEquals(6381, uris.get(2).getPort());
    }

    @Test
    void shouldIgnoreEmptyReplicaEntries() {
        RedisProperties props = buildBase();
        props.setHostReplicas("replica1:6380, ,, replica2:6381,");

        List<RedisURI> uris = props.getAllURIs();

        // primario + 2 réplicas válidas, las entradas vacías se filtran
        assertEquals(3, uris.size());
        assertEquals("replica1", uris.get(1).getHost());
        assertEquals("replica2", uris.get(2).getHost());
    }

    @Test
    void shouldUseDefaultPortForReplicaWithoutExplicitPort() {
        RedisProperties props = buildBase();
        props.setPort(6379);
        props.setHostReplicas("replica1");

        List<RedisURI> uris = props.getAllURIs();

        assertEquals(2, uris.size());
        assertEquals("replica1", uris.get(1).getHost());
        assertEquals(6379, uris.get(1).getPort());
    }

    @Test
    void shouldPropagateAuthenticationAndSslToAllUris() {
        RedisProperties props = buildBase();
        props.setUsername("user");
        props.setPassword("secret");
        props.setUseSsl(true);
        props.setHostReplicas("replica1:6380");

        List<RedisURI> uris = props.getAllURIs();

        for (RedisURI uri : uris) {
            StepVerifier.create(uri.getCredentialsProvider().resolveCredentials())
                    .assertNext(credentials -> {
                        assertEquals("user", credentials.getUsername());
                        assertArrayEquals("secret".toCharArray(), credentials.getPassword());
                        assertTrue(uri.isSsl());
                    })
                    .verifyComplete();
        }
    }
}
