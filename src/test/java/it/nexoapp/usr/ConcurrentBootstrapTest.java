package it.nexoapp.usr;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two simultaneous first logins of the same President create exactly one identity link and one PRESIDENT
 * membership (T-01-19): the association row lock serializes them and the second one finds the first one's link.
 */
@QuarkusTest
@TestProfile(ConcurrentBootstrapProfile.class)
class ConcurrentBootstrapTest {

    /** Number of simultaneous first logins. */
    private static final int PARALLEL_LOGINS = 2;

    /* - keycloak - */
    private final KeycloakTestClient keycloak = new KeycloakTestClient();

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Starts every test from a platform with no linked identity of the association.
     */
    @BeforeEach
    void cleanLinks() {
        LinkedIdentityFixture.clean(entityManager, ConcurrentBootstrapProfile.ASSOCIATION_ID);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Both parallel requests answer linked with the same member, and the database holds one link, one
     * membership and one PRESIDENT role.
     *
     * @throws Exception when a request fails
     */
    @Test
    void parallelFirstLoginsLinkOnce() throws Exception {
        final String token = keycloak.getAccessToken("vice-president", "password", "nexo-dev");
        final CyclicBarrier start = new CyclicBarrier(PARALLEL_LOGINS);
        final Callable<String> login = () -> {
            start.await();
            final var body = given().auth().oauth2(token)
                    .when().get("/me")
                    .then().statusCode(200)
                    .extract().jsonPath();
            return body.getString("linked") + ":" + body.getString("memberId");
        };

        final List<String> results;
        try (ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_LOGINS)) {
            final List<Future<String>> futures = executor.invokeAll(List.of(login, login));
            results = futures.stream().map(ConcurrentBootstrapTest::get).toList();
        }

        assertEquals(PARALLEL_LOGINS, results.size());
        assertEquals(results.getFirst(), results.getLast(), "both logins must see the same member");
        assertEquals(true, results.getFirst().startsWith("true:"), "both logins must be linked");
        assertEquals(1, LinkedIdentityFixture.count(entityManager, "identity_link",
                ConcurrentBootstrapProfile.ASSOCIATION_ID));
        assertEquals(1, LinkedIdentityFixture.count(entityManager, "membership",
                ConcurrentBootstrapProfile.ASSOCIATION_ID));
        assertEquals(1, LinkedIdentityFixture.countRole(entityManager, "PRESIDENT",
                ConcurrentBootstrapProfile.ASSOCIATION_ID));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Unwraps a future without checked exceptions.
     *
     * @param future the finished future
     * @param <T>    result type
     * @return the result
     */
    private static <T> T get(final Future<T> future) {
        try {
            return future.get();
        } catch (final Exception e) {
            throw new IllegalStateException("parallel login failed", e);
        }
    }

}
