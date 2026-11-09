package it.nexoapp.usr;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import it.nexoapp.common.tenant.TenantContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Walking skeleton of the platform: the President configured in properties logs in with a real Keycloak token,
 * is linked once to a PRESIDENT membership, and a stranger can ask who they are without gaining any access.
 */
@QuarkusTest
class PresidentLoginTest {

    /** Fixed subject of the realm user {@code president}. */
    private static final String PRESIDENT_SUBJECT = "00000000-0000-4000-8000-000000000001";

    /** Fixed subject of the realm user {@code stranger}. */
    private static final String STRANGER_SUBJECT = "00000000-0000-4000-8000-000000000009";

    /** Association configured for the test profile. */
    private static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000a";

    /* - keycloak - */
    private final KeycloakTestClient keycloak = new KeycloakTestClient();

    /* - configuration - */
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String issuer;

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Starts every test from a platform with no linked identity of the association.
     */
    @BeforeEach
    void cleanLinks() {
        unscoped(() -> {
            entityManager.createNativeQuery("DELETE FROM users.identity_link WHERE tenant_id = CAST(:t AS UUID)")
                    .setParameter("t", ASSOCIATION_ID).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users.membership_role WHERE tenant_id = CAST(:t AS UUID)")
                    .setParameter("t", ASSOCIATION_ID).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users.membership WHERE tenant_id = CAST(:t AS UUID)")
                    .setParameter("t", ASSOCIATION_ID).executeUpdate();
            return null;
        });
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The first login of the President links the identity to a new PRESIDENT membership of the configured
     * association, with rows attributed to the IdP actor.
     */
    @Test
    void presidentFirstLoginIsLinked() {
        final String token = keycloak.getAccessToken("president", "password", "nexo-dev");

        given().auth().oauth2(token)
                .when().get("/me")
                .then().statusCode(200)
                .body("linked", equalTo(true))
                .body("associationId", equalTo(ASSOCIATION_ID))
                .body("memberId", notNullValue())
                .body("roles", contains("PRESIDENT"));

        assertEquals(1, count("identity_link WHERE subject = :s AND issuer = :i AND created_by = :a AND updated_by = :a"));
        assertEquals(1, count("membership WHERE created_by = :a AND tenant_id = CAST('" + ASSOCIATION_ID + "' AS UUID)"));
        assertEquals(1, count("membership_role WHERE role = 'PRESIDENT' AND created_by = :a"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Later logins resolve through the identity link only: nothing is created again and the member stays the same.
     */
    @Test
    void laterLoginsAreStable() {
        final String token = keycloak.getAccessToken("president", "password", "nexo-dev");

        final String memberId = given().auth().oauth2(token)
                .when().get("/me")
                .then().statusCode(200).body("linked", equalTo(true))
                .extract().path("memberId");

        for (int i = 0; i < 3; i++) {
            given().auth().oauth2(token)
                    .when().get("/me")
                    .then().statusCode(200)
                    .body("memberId", equalTo(memberId))
                    .body("roles", contains("PRESIDENT"));
        }

        assertEquals(1, count("identity_link WHERE subject = :s AND issuer = :i"));
        assertEquals(1, count("membership_role WHERE role = 'PRESIDENT'"));
        assertEquals(1, count("membership WHERE tenant_id = CAST('" + ASSOCIATION_ID + "' AS UUID)"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A stranger sees linked=false and nothing is linked for the stranger.
     */
    @Test
    void strangerIsNotLinked() {
        final String token = keycloak.getAccessToken("stranger", "password", "nexo-dev");

        given().auth().oauth2(token)
                .when().get("/me")
                .then().statusCode(200)
                .body("linked", equalTo(false))
                .body("associationId", nullValue())
                .body("memberId", nullValue())
                .body("roles", empty());

        final Number links = unscoped(() -> (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM users.identity_link WHERE subject = :s")
                .setParameter("s", STRANGER_SUBJECT).getSingleResult());
        assertEquals(0, links.intValue());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The linked President reads the configured association: the profile carries exactly the public fields and
     * no audit column or tenant identifier.
     */
    @Test
    void presidentReadsTheAssociationProfile() {
        final String token = keycloak.getAccessToken("president", "password", "nexo-dev");

        given().auth().oauth2(token)
                .when().get("/association")
                .then().statusCode(200)
                .body("keySet()", containsInAnyOrder(
                        "id", "name", "taxCode", "registeredOffice", "affiliationBody", "affiliationCode"))
                .body("id", equalTo(ASSOCIATION_ID))
                .body("name", equalTo("ASD Nexo Test"))
                .body("taxCode", equalTo("97000000010"))
                .body("registeredOffice.keySet()", containsInAnyOrder("street", "zipCode", "city", "province", "state"))
                .body("registeredOffice.street", equalTo("Via Test 10"))
                .body("registeredOffice.zipCode", equalTo("20100"))
                .body("registeredOffice.city", equalTo("Milano"))
                .body("registeredOffice.province", equalTo("MI"))
                .body("registeredOffice.state", equalTo("IT"))
                .body("affiliationBody", nullValue())
                .body("affiliationCode", nullValue());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A stranger (valid token, no membership) is refused by the identity gate and nothing is linked for them.
     */
    @Test
    void strangerCannotReadTheAssociation() {
        final String token = keycloak.getAccessToken("stranger", "password", "nexo-dev");

        given().auth().oauth2(token)
                .when().get("/association")
                .then().statusCode(403)
                .body("code", equalTo("IDENTITY_NOT_LINKED"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A call without any token is unauthorized.
     */
    @Test
    void anonymousCannotReadTheAssociation() {
        given()
                .when().get("/association")
                .then().statusCode(401);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Counts the rows of a table fragment, binding the President subject, the issuer and the idp actor.
     *
     * @param fragment table name and where clause
     * @return the number of rows
     */
    private int count(final String fragment) {
        final Number value = unscoped(() -> {
            final var query = entityManager.createNativeQuery("SELECT count(*) FROM users." + fragment);
            if (fragment.contains(":s")) {
                query.setParameter("s", PRESIDENT_SUBJECT);
            }
            if (fragment.contains(":i")) {
                query.setParameter("i", issuer);
            }
            if (fragment.contains(":a")) {
                query.setParameter("a", "idp:" + PRESIDENT_SUBJECT);
            }
            return (Number) query.getSingleResult();
        });
        return value.intValue();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Runs a database task unscoped and in its own transaction (native SQL is never tenant-filtered).
     *
     * @param task the task
     * @param <T>  result type
     * @return the task result
     */
    private <T> T unscoped(final Supplier<T> task) {
        return TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew().call(task::get));
    }

}
