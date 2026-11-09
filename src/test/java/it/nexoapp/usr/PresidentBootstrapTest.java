package it.nexoapp.usr;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import it.nexoapp.common.security.IdentityNotLinkedException;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Only the verified, configured President is ever linked (ROL-04, D-03 amended 2026-11-08): a verified user whose
 * email differs from the configured one stays unlinked, and the refusal reveals nothing about why (D-04).
 */
@QuarkusTest
class PresidentBootstrapTest {

    /** Association configured for the default test profile. */
    static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000a";

    /** Message of the gate refusal, identical for every unlinked caller whatever the reason (D-04). */
    static final String NOT_LINKED_MESSAGE = IdentityNotLinkedException.MESSAGE;

    /** Code of the gate refusal. */
    static final String NOT_LINKED_CODE = IdentityNotLinkedException.CODE;

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
        LinkedIdentityFixture.clean(entityManager, ASSOCIATION_ID);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A verified stranger asks who they are: not linked, no ids, no roles, and nothing is created for them.
     */
    @Test
    void strangerStaysUnlinkedOnMe() {
        given().auth().oauth2(token("stranger"))
                .when().get("/me")
                .then().statusCode(200)
                .body("linked", equalTo(false))
                .body("associationId", nullValue())
                .body("memberId", nullValue())
                .body("roles", empty());

        assertEquals(0, LinkedIdentityFixture.linksOfSubject(entityManager, LinkedIdentityFixture.STRANGER_SUBJECT));
        assertEquals(0, LinkedIdentityFixture.count(entityManager, "membership", ASSOCIATION_ID));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A verified stranger is refused by the identity gate with the neutral not-linked fault.
     */
    @Test
    void strangerIsRefusedOnTheAssociation() {
        given().auth().oauth2(token("stranger"))
                .when().get("/association")
                .then().statusCode(403)
                .body("code", equalTo(NOT_LINKED_CODE))
                .body("message", equalTo(NOT_LINKED_MESSAGE));

        assertEquals(0, LinkedIdentityFixture.linksOfSubject(entityManager, LinkedIdentityFixture.STRANGER_SUBJECT));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A stranger that tried before does not prevent the real President from being linked afterwards, and the
     * President is the only identity linked.
     */
    @Test
    void presidentIsLinkedAfterAStrangerTried() {
        given().auth().oauth2(token("stranger")).when().get("/me").then().statusCode(200);

        given().auth().oauth2(token("president"))
                .when().get("/me")
                .then().statusCode(200)
                .body("linked", equalTo(true))
                .body("roles", contains("PRESIDENT"));

        assertEquals(1, LinkedIdentityFixture.count(entityManager, "identity_link", ASSOCIATION_ID));
        assertEquals(1, LinkedIdentityFixture.countRole(entityManager, "PRESIDENT", ASSOCIATION_ID));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Repeated logins of the President keep one link, one membership and one role.
     */
    @Test
    void repeatedLoginsLinkOnce() {
        final String token = token("president");

        for (int i = 0; i < 4; i++) {
            given().auth().oauth2(token).when().get("/me").then().statusCode(200).body("linked", equalTo(true));
        }

        assertEquals(1, LinkedIdentityFixture.count(entityManager, "identity_link", ASSOCIATION_ID));
        assertEquals(1, LinkedIdentityFixture.count(entityManager, "membership", ASSOCIATION_ID));
        assertEquals(1, LinkedIdentityFixture.countRole(entityManager, "PRESIDENT", ASSOCIATION_ID));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Gets a real access token of a realm user.
     *
     * @param user realm user name
     * @return the access token
     */
    private String token(final String user) {
        return keycloak.getAccessToken(user, "password", "nexo-dev");
    }

}
