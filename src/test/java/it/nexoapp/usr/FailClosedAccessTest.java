package it.nexoapp.usr;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import it.nexoapp.common.security.AssociationSelectionRequiredException;
import it.nexoapp.common.tenant.TenantNotResolvedException;
import it.nexoapp.usr.api.association.service.AssociationService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Access fails closed (ROL-02, D-02): no token is 401, a header naming a foreign association is refused, and a
 * service call whose tenant cannot be resolved never falls back to the configured association.
 */
@QuarkusTest
class FailClosedAccessTest {

    /* - keycloak - */
    private final KeycloakTestClient keycloak = new KeycloakTestClient();

    /* - service - */
    @Inject
    AssociationService associationService;

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Starts every test from a platform with no linked identity of the association.
     */
    @BeforeEach
    void cleanLinks() {
        LinkedIdentityFixture.clean(entityManager, PresidentBootstrapTest.ASSOCIATION_ID);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A request without credentials never reaches the association.
     */
    @Test
    void requestWithoutTokenIsUnauthorized() {
        given()
                .when().get("/association")
                .then().statusCode(401);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The association header is never trusted alone: a linked President naming a foreign association is refused.
     */
    @Test
    void foreignAssociationHeaderIsRefused() {
        final String token = keycloak.getAccessToken("president", "password", "nexo-dev");

        given().auth().oauth2(token)
                .header("x-association-id", UUID.randomUUID().toString())
                .when().get("/association")
                .then().statusCode(403)
                .body("code", equalTo(AssociationSelectionRequiredException.CODE));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * On a bare thread with a request context but no linked caller and no tenant scope, the service call fails
     * with {@link TenantNotResolvedException}: there is no default association.
     *
     * @throws Exception when the executor fails unexpectedly
     */
    @Test
    void serviceCallWithoutResolvableTenantFails() throws Exception {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            final ExecutionException failure = assertThrows(ExecutionException.class, () -> executor.submit(() -> {
                final ManagedContext requestContext = Arc.container().requestContext();
                requestContext.activate();
                try {
                    return associationService.getCurrent();
                } finally {
                    requestContext.terminate();
                }
            }).get());

            assertTrue(hasCause(failure, TenantNotResolvedException.class),
                    "expected TenantNotResolvedException in the cause chain but was " + failure);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Looks for a type in the cause chain.
     *
     * @param failure the failure
     * @param type    the type searched
     * @return true when the type is in the chain
     */
    private static boolean hasCause(final Throwable failure, final Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

}
