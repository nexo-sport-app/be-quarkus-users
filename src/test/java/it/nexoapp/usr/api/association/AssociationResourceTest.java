package it.nexoapp.usr.api.association;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import it.nexoapp.common.security.Role;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.LinkedIdentityFixture;
import it.nexoapp.usr.api.membership.service.UsersTestFixtures;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The President updates the association profile; other callers cannot, and a tax code collision is a clean 409.
 * Every test restores the configured values of the test association, which other tests assert.
 */
@QuarkusTest
class AssociationResourceTest {

    /** Association configured for the test profile. */
    private static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000a";

    /** Second association, owned by nobody in these tests. */
    private static final UUID OTHER_ASSOCIATION_ID = UUID.fromString("a55d0000-0000-4000-8000-00000000000d");

    /** Name of the second association, which must never leak. */
    private static final String OTHER_ASSOCIATION_NAME = "ASD Altra Societa";

    /** Tax code of the second association. */
    private static final String OTHER_TAX_CODE = "97000000002";

    /** Fixed subject of the realm user {@code secretary}. */
    private static final String SECRETARY_SUBJECT = "00000000-0000-4000-8000-000000000003";

    /* - keycloak - */
    private final KeycloakTestClient keycloak = new KeycloakTestClient();

    /* - fixtures - */
    @Inject
    UsersTestFixtures fixtures;

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Starts from a clean association A: the President is linked by a first login, the secretary is linked by
     * the fixture, and association B exists.
     */
    @BeforeEach
    void prepare() {
        LinkedIdentityFixture.clean(entityManager, ASSOCIATION_ID);
        given().auth().oauth2(token("president")).when().get("/me").then().statusCode(200).body("linked", equalTo(true));
        fixtures.linkMember(SECRETARY_SUBJECT, UUID.fromString(ASSOCIATION_ID), Role.SECRETARY);
        fixtures.createAssociation(OTHER_ASSOCIATION_ID, OTHER_ASSOCIATION_NAME, OTHER_TAX_CODE);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Gives association A back its configured values.
     */
    @AfterEach
    void restore() {
        fixtures.restoreTestAssociation();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The President updates every field and reads the new values; the row is attributed to the President member.
     */
    @Test
    void presidentUpdatesTheProfile() {
        final String memberId = given().auth().oauth2(token("president")).when().get("/me")
                .then().statusCode(200).extract().path("memberId");

        given().auth().oauth2(token("president")).contentType(ContentType.JSON)
                .body(body("ASD Nuova", "97000000099", "33333", "Associazione Sportiva", "AS-77"))
                .when().put("/association")
                .then().statusCode(200)
                .body("id", equalTo(ASSOCIATION_ID))
                .body("name", equalTo("ASD Nuova"))
                .body("taxCode", equalTo("97000000099"))
                .body("registeredOffice.zipCode", equalTo("33333"))
                .body("affiliationBody", equalTo("Associazione Sportiva"))
                .body("affiliationCode", equalTo("AS-77"));

        given().auth().oauth2(token("president"))
                .when().get("/association")
                .then().statusCode(200)
                .body("name", equalTo("ASD Nuova"))
                .body("taxCode", equalTo("97000000099"))
                .body("registeredOffice.street", equalTo("Via Nuova 5"))
                .body("affiliationCode", equalTo("AS-77"));

        assertEquals(memberId.toLowerCase(), rawColumn("updated_by"), "the update is attributed to the President member");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * An accented UTF-8 name is stored and returned unchanged.
     */
    @Test
    void accentedNameRoundTrips() {
        final String name = "Società Sportiva Dilettantistica Città di Brà";

        given().auth().oauth2(token("president")).contentType(ContentType.JSON.withCharset("UTF-8"))
                .body(body(name, "97000000010", "20100", null, null))
                .when().put("/association")
                .then().statusCode(200)
                .body("name", equalTo(name))
                .body("affiliationBody", nullValue());

        given().auth().oauth2(token("president"))
                .when().get("/association")
                .then().statusCode(200).body("name", equalTo(name));
        assertEquals(name, rawColumn("name"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A linked member without the PRESIDENT role reads the profile but cannot write it.
     */
    @Test
    void secretaryReadsButCannotWrite() {
        given().auth().oauth2(token("secretary"))
                .when().get("/association")
                .then().statusCode(200).body("name", equalTo("ASD Nexo Test"));

        given().auth().oauth2(token("secretary")).contentType(ContentType.JSON)
                .body(body("ASD Hacked", "97000000010", "20100", null, null))
                .when().put("/association")
                .then().statusCode(403).body("code", equalTo("FORBIDDEN"));

        given().auth().oauth2(token("president"))
                .when().get("/association")
                .then().statusCode(200).body("name", equalTo("ASD Nexo Test"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A stranger (valid token, no membership) is refused by the identity gate.
     */
    @Test
    void strangerCannotWrite() {
        given().auth().oauth2(token("stranger")).contentType(ContentType.JSON)
                .body(body("ASD Hacked", "97000000010", "20100", null, null))
                .when().put("/association")
                .then().statusCode(403).body("code", equalTo("IDENTITY_NOT_LINKED"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A tax code already registered to another association is a 409 that reveals nothing about it, and the
     * caller's row is unchanged.
     */
    @Test
    void taxCodeOfAnotherAssociationIsAConflict() {
        final ValidatableResponse response = given().auth().oauth2(token("president")).contentType(ContentType.JSON)
                .body(body("ASD Collisione", OTHER_TAX_CODE, "20100", null, null))
                .when().put("/association")
                .then().statusCode(409)
                .body("code", equalTo("ASSOCIATION_TAX_CODE_CONFLICT"));

        final String raw = response.extract().asString();
        assertFalse(raw.contains(OTHER_ASSOCIATION_NAME), "the other name must not leak");
        assertFalse(raw.contains(OTHER_ASSOCIATION_ID.toString()), "the other id must not leak");

        given().auth().oauth2(token("president"))
                .when().get("/association")
                .then().statusCode(200)
                .body("name", equalTo("ASD Nexo Test"))
                .body("taxCode", equalTo("97000000010"))
                .body("name", not(containsString("Collisione")));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * After the tests the configured values are back (the restore hook works, other tests rely on it).
     */
    @Test
    void restoreGivesTheConfiguredValuesBack() {
        given().auth().oauth2(token("president")).contentType(ContentType.JSON)
                .body(body("ASD Temporanea", "97000000098", "20100", "Body", "Code"))
                .when().put("/association").then().statusCode(200);

        fixtures.restoreTestAssociation();

        given().auth().oauth2(token("president"))
                .when().get("/association")
                .then().statusCode(200)
                .body("name", equalTo("ASD Nexo Test"))
                .body("taxCode", equalTo("97000000010"))
                .body("affiliationBody", nullValue())
                .body("affiliationCode", nullValue());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Gets an access token of a realm user.
     *
     * @param user realm user name
     * @return the access token
     */
    private String token(final String user) {
        return keycloak.getAccessToken(user, "password", "nexo-dev");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Builds a valid update body.
     *
     * @param name            association name
     * @param taxCode         tax code
     * @param zipCode         postal code
     * @param affiliationBody affiliation body, may be null
     * @param affiliationCode affiliation code, may be null
     * @return the request body as a map
     */
    private static Map<String, Object> body(final String name, final String taxCode, final String zipCode,
                                            final String affiliationBody, final String affiliationCode) {
        final Map<String, Object> office = Map.of("street", "Via Nuova 5", "zipCode", zipCode,
                "city", "Roma", "province", "RM", "state", "IT");
        final Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("taxCode", taxCode);
        body.put("office", office);
        body.put("affiliationBody", affiliationBody);
        body.put("affiliationCode", affiliationCode);
        return body;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Reads one column of the row of association A, natively and unscoped.
     *
     * @param column column name
     * @return its value
     */
    private String rawColumn(final String column) {
        return TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew().call(() -> (String) entityManager
                .createNativeQuery("SELECT CAST(" + column + " AS TEXT) FROM users.association WHERE id = CAST(:id AS UUID)")
                .setParameter("id", ASSOCIATION_ID).getSingleResult()));
    }

}
