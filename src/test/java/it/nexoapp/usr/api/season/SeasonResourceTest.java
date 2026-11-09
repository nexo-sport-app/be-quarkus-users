package it.nexoapp.usr.api.season;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.http.ContentType;
import it.nexoapp.common.security.Role;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.LinkedIdentityFixture;
import it.nexoapp.usr.api.membership.service.UsersTestFixtures;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Seasons over REST for the President of association A. Every test removes the seasons of A before and after it
 * runs and creates its own preconditions, so the result never depends on method or class order.
 */
@QuarkusTest
class SeasonResourceTest {

    /** Association configured for the test profile. */
    private static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000a";

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
     * Starts from a linked President and Secretary of association A with no season.
     */
    @BeforeEach
    void prepare() {
        LinkedIdentityFixture.clean(entityManager, ASSOCIATION_ID);
        given().auth().oauth2(token("president")).when().get("/me").then().statusCode(200).body("linked", equalTo(true));
        fixtures.linkMember(SECRETARY_SUBJECT, UUID.fromString(ASSOCIATION_ID), Role.SECRETARY);
        deleteSeasons();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Removes the seasons created by the test.
     */
    @AfterEach
    void cleanUp() {
        deleteSeasons();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A start year alone gives the default season from 1 September to 31 August of the next year.
     */
    @Test
    void startYearGivesTheDefaultSeason() {
        given().auth().oauth2(token("president")).contentType(ContentType.JSON).body(Map.of("startYear", 2026))
                .when().post("/seasons")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("label", equalTo("2026/2027"))
                .body("startDate", equalTo("2026-09-01"))
                .body("endDate", equalTo("2027-08-31"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Two explicit ranges that touch without sharing a day are both accepted.
     */
    @Test
    void adjacentRangesAreAccepted() {
        create(range("2027-09-01", "2028-08-31")).then().statusCode(201).body("label", equalTo("2027/2028"));
        create(range("2028-09-01", "2029-08-31")).then().statusCode(201).body("label", equalTo("2028/2029"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A range that shares a day with an existing season is a 409 SEASON_OVERLAP, whether identical or partial.
     */
    @Test
    void overlappingRangesAreRefused() {
        create(range("2029-09-01", "2030-08-31")).then().statusCode(201);

        create(range("2029-09-01", "2030-08-31")).then().statusCode(409).body("code", equalTo("SEASON_OVERLAP"));
        create(range("2030-01-01", "2030-06-30")).then().statusCode(409).body("code", equalTo("SEASON_OVERLAP"));

        given().auth().oauth2(token("president")).when().get("/seasons")
                .then().statusCode(200).body("$", hasSize(1));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Invalid bodies yield a 400 Fault that lists the violation: end before start, nothing given, both forms given.
     */
    @Test
    void invalidRequestsAreViolations() {
        create(range("2028-01-01", "2027-01-01")).then().statusCode(400)
                .body("code", equalTo("nexo.validation"))
                .body("violations.field", hasItem("endAfterStart"));

        create(Map.of()).then().statusCode(400)
                .body("code", equalTo("nexo.validation"))
                .body("violations.field", hasItem("rangeSpecified"));

        create(Map.of("startYear", 2026, "startDate", "2026-09-01", "endDate", "2027-08-31")).then().statusCode(400)
                .body("code", equalTo("nexo.validation"))
                .body("violations.field", hasItem("rangeSpecified"));

        given().auth().oauth2(token("president")).when().get("/seasons")
                .then().statusCode(200).body("$", hasSize(0));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The listing is ordered by start date ascending whatever the creation order.
     */
    @Test
    void listIsOrderedByStartDate() {
        create(Map.of("startYear", 2028)).then().statusCode(201);
        create(Map.of("startYear", 2026)).then().statusCode(201);
        create(Map.of("startYear", 2027)).then().statusCode(201);

        given().auth().oauth2(token("president")).when().get("/seasons")
                .then().statusCode(200)
                .body("label", contains("2026/2027", "2027/2028", "2028/2029"))
                .body("startDate", contains("2026-09-01", "2027-09-01", "2028-09-01"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A secretary lists seasons but cannot create one, and nothing is written by the refused request.
     */
    @Test
    void secretaryReadsButCannotCreate() {
        given().auth().oauth2(token("secretary")).contentType(ContentType.JSON).body(Map.of("startYear", 2026))
                .when().post("/seasons")
                .then().statusCode(403).body("code", equalTo("FORBIDDEN"));

        given().auth().oauth2(token("secretary")).when().get("/seasons")
                .then().statusCode(200).body("$", hasSize(0));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A valid token without a membership is refused by the identity gate for both operations.
     */
    @Test
    void strangerIsRefused() {
        given().auth().oauth2(token("stranger")).when().get("/seasons")
                .then().statusCode(403).body("code", equalTo("IDENTITY_NOT_LINKED"));
        given().auth().oauth2(token("stranger")).contentType(ContentType.JSON).body(Map.of("startYear", 2026))
                .when().post("/seasons")
                .then().statusCode(403).body("code", equalTo("IDENTITY_NOT_LINKED"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Posts a season creation as the President.
     *
     * @param body the request body
     * @return the response
     */
    private io.restassured.response.Response create(final Map<String, Object> body) {
        return given().auth().oauth2(token("president")).contentType(ContentType.JSON).body(body)
                .when().post("/seasons");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Builds an explicit range body.
     *
     * @param start first day
     * @param end   last day
     * @return the request body
     */
    private static Map<String, Object> range(final String start, final String end) {
        return Map.of("startDate", start, "endDate", end);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Deletes every season of association A, natively and bound to its tenant.
     */
    private void deleteSeasons() {
        TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew().call(() -> entityManager
                .createNativeQuery("DELETE FROM users.season WHERE tenant_id = CAST(:t AS UUID)")
                .setParameter("t", ASSOCIATION_ID).executeUpdate()));
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

}
