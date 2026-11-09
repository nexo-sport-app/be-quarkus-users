package it.nexoapp.usr;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An unverified email is never enough, even when it matches the configured President email exactly (T-01-18,
 * D-04): the refusal is the same as for any other unlinked caller.
 */
@QuarkusTest
@TestProfile(UnverifiedBootstrapProfile.class)
class UnverifiedBootstrapTest {

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
        LinkedIdentityFixture.clean(entityManager, UnverifiedBootstrapProfile.ASSOCIATION_ID);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The unverified user matching the configured email stays unlinked on {@code /me} and nothing is created.
     */
    @Test
    void unverifiedEmailIsNotLinkedOnMe() {
        given().auth().oauth2(token())
                .when().get("/me")
                .then().statusCode(200)
                .body("linked", equalTo(false))
                .body("associationId", nullValue())
                .body("memberId", nullValue())
                .body("roles", empty());

        assertEquals(0, LinkedIdentityFixture.linksOfSubject(entityManager, LinkedIdentityFixture.UNVERIFIED_SUBJECT));
        assertEquals(0, LinkedIdentityFixture.count(entityManager, "membership",
                UnverifiedBootstrapProfile.ASSOCIATION_ID));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The refusal on {@code /association} carries the same code and message as the one of a plain stranger, so
     * a caller cannot tell an unverified match from a mismatch (D-04).
     */
    @Test
    void unverifiedEmailGetsTheSameRefusalAsAStranger() {
        given().auth().oauth2(token())
                .when().get("/association")
                .then().statusCode(403)
                .body("code", equalTo(PresidentBootstrapTest.NOT_LINKED_CODE))
                .body("message", equalTo(PresidentBootstrapTest.NOT_LINKED_MESSAGE));

        assertEquals(0, LinkedIdentityFixture.linksOfSubject(entityManager, LinkedIdentityFixture.UNVERIFIED_SUBJECT));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Gets a real access token of the unverified realm user.
     *
     * @return the access token
     */
    private String token() {
        return keycloak.getAccessToken("unverified", "password", "nexo-dev");
    }

}
