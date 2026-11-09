package it.nexoapp.usr.api;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Endpoint-level isolation between two associations with real tokens: the Presidents of A ({@code president}) and
 * B ({@code councillor}) see and change only their own association, seasons and identity. Every method creates its
 * own preconditions with a start year no other method uses and removes the seasons of both associations before and
 * after it runs, so the outcome never depends on the order of the methods.
 */
@QuarkusTest
class AssociationSeasonTwoTenantTest {

    /** Association A, configured for the test profile. */
    private static final UUID ASSOCIATION_A = UUID.fromString("a55d0000-0000-4000-8000-00000000000a");

    /** Association B, the second tenant of the users tests. */
    private static final UUID ASSOCIATION_B = UUID.fromString("a55d0000-0000-4000-8000-00000000000d");

    /** Name given to B when the fixture creates it. */
    private static final String B_NAME = "ASD Nexo B";

    /** Tax code of B. */
    private static final String B_TAX_CODE = "97000000002";

    /** Fixed subject of the realm user {@code councillor}, the President of B. */
    private static final String COUNCILLOR_SUBJECT = "00000000-0000-4000-8000-000000000004";

    /** Name of the request header that selects the association. */
    private static final String ASSOCIATION_HEADER = "x-association-id";

    /* - keycloak - */
    private final KeycloakTestClient keycloak = new KeycloakTestClient();

    /* - fixtures - */
    @Inject
    UsersTestFixtures fixtures;

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    /** Member of the President of B, as returned by the fixture link. */
    private UUID memberOfB;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Starts from linked Presidents of A and B and no season in either association.
     */
    @BeforeEach
    void prepare() {
        LinkedIdentityFixture.clean(entityManager, ASSOCIATION_A.toString());
        given().auth().oauth2(token("president")).when().get("/me").then().statusCode(200).body("linked", equalTo(true));
        fixtures.createAssociation(ASSOCIATION_B, B_NAME, B_TAX_CODE);
        memberOfB = fixtures.linkMember(COUNCILLOR_SUBJECT, ASSOCIATION_B, Role.PRESIDENT);
        deleteSeasons();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Removes the seasons created by the test and gives association A its configured values back.
     */
    @AfterEach
    void cleanUp() {
        deleteSeasons();
        fixtures.restoreTestAssociation();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Each President is told their own association and member, both as PRESIDENT, and the members differ.
     */
    @Test
    void meIsPerAssociation() {
        final Response a = given().auth().oauth2(token("president")).when().get("/me");
        final Response b = given().auth().oauth2(token("councillor")).when().get("/me");
        a.then().statusCode(200).body("linked", equalTo(true)).body("roles", equalTo(List.of("PRESIDENT")));
        b.then().statusCode(200).body("linked", equalTo(true)).body("roles", equalTo(List.of("PRESIDENT")));

        assertEquals(ASSOCIATION_A.toString(), a.path("associationId"));
        assertEquals(ASSOCIATION_B.toString(), b.path("associationId"));
        assertEquals(memberOfB.toString(), b.path("memberId"));
        final String memberOfA = a.path("memberId");
        assertNotEquals(memberOfA, b.path("memberId"), "the two Presidents are different members");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Each President reads only their own association and the body never mentions the other one.
     */
    @Test
    void associationReadIsPerAssociation() {
        final Response a = given().auth().oauth2(token("president")).when().get("/association");
        final Response b = given().auth().oauth2(token("councillor")).when().get("/association");
        a.then().statusCode(200).body("id", equalTo(ASSOCIATION_A.toString()));
        b.then().statusCode(200).body("id", equalTo(ASSOCIATION_B.toString()));

        final String bodyA = a.asString();
        final String bodyB = b.asString();
        for (final String foreign : new String[]{b.path("name"), b.path("taxCode"), ASSOCIATION_B.toString()}) {
            assertFalse(bodyA.contains(foreign), "A must not see " + foreign);
        }
        for (final String foreign : new String[]{a.path("name"), a.path("taxCode"), ASSOCIATION_A.toString()}) {
            assertFalse(bodyB.contains(foreign), "B must not see " + foreign);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * An update by one President never changes the other association, in both directions; the values of both
     * associations are restored at the end.
     */
    @Test
    void associationWriteIsPerAssociation() {
        final Map<String, Object> originalB = updateBody(given().auth().oauth2(token("councillor")).when().get("/association"));
        final String nameOfA = given().auth().oauth2(token("president")).when().get("/association").path("name");
        final String nameOfB = (String) originalB.get("name");
        try {
            put("councillor", withName(originalB, "ASD B Rinominata")).then().statusCode(200)
                    .body("id", equalTo(ASSOCIATION_B.toString()))
                    .body("name", equalTo("ASD B Rinominata"));
            given().auth().oauth2(token("president")).when().get("/association")
                    .then().statusCode(200).body("name", equalTo(nameOfA));

            final Map<String, Object> bodyA = updateBody(given().auth().oauth2(token("president")).when().get("/association"));
            put("president", withName(bodyA, "ASD A Rinominata")).then().statusCode(200)
                    .body("id", equalTo(ASSOCIATION_A.toString()))
                    .body("name", equalTo("ASD A Rinominata"));
            given().auth().oauth2(token("councillor")).when().get("/association")
                    .then().statusCode(200).body("name", equalTo("ASD B Rinominata"));
        } finally {
            put("councillor", originalB).then().statusCode(200).body("name", equalTo(nameOfB));
            fixtures.restoreTestAssociation();
        }
        given().auth().oauth2(token("councillor")).when().get("/association").then().body("name", equalTo(nameOfB));
        given().auth().oauth2(token("president")).when().get("/association").then().body("name", equalTo(nameOfA));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The same start year is accepted in both associations: uniqueness is per association.
     */
    @Test
    void sameStartYearWorksInBothAssociations() {
        postSeason("president", 2031).then().statusCode(201).body("label", equalTo("2031/2032"));
        postSeason("councillor", 2031).then().statusCode(201).body("label", equalTo("2031/2032"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The overlap rule only considers the seasons of the caller's association.
     */
    @Test
    void overlapIsScopedToTheAssociation() {
        postSeason("president", 2032).then().statusCode(201);

        final Map<String, Object> partial = Map.of("startDate", "2032-10-01", "endDate", "2033-06-30");
        postSeasonBody("councillor", partial).then().statusCode(201);
        postSeasonBody("president", partial).then().statusCode(409).body("code", equalTo("SEASON_OVERLAP"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The season listings of the two associations are disjoint and each holds its own season.
     */
    @Test
    void seasonListsAreDisjoint() {
        final String idA = postSeason("president", 2033).then().statusCode(201).extract().path("id");
        final String idB = postSeason("councillor", 2033).then().statusCode(201).extract().path("id");
        assertNotEquals(idA, idB);

        final Set<String> idsA = seasonIds("president");
        final Set<String> idsB = seasonIds("councillor");

        assertTrue(idsA.contains(idA), "A lists its own season");
        assertTrue(idsB.contains(idB), "B lists its own season");
        final Set<String> common = new HashSet<>(idsA);
        common.retainAll(idsB);
        assertTrue(common.isEmpty(), "the two listings share no season: " + common);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A President who names the other association in the header is refused everywhere and nothing is written.
     */
    @Test
    void foreignHeaderIsRefusedAndWritesNothing() {
        final String nameOfB = given().auth().oauth2(token("councillor")).when().get("/association").path("name");
        final Map<String, Object> bodyOfA = updateBody(given().auth().oauth2(token("president")).when().get("/association"));

        given().auth().oauth2(token("president")).header(ASSOCIATION_HEADER, ASSOCIATION_B.toString())
                .when().get("/association")
                .then().statusCode(403).body("code", equalTo("ASSOCIATION_SELECTION_REQUIRED"));
        given().auth().oauth2(token("president")).header(ASSOCIATION_HEADER, ASSOCIATION_B.toString())
                .contentType(ContentType.JSON).body(withName(bodyOfA, "ASD Intrusa"))
                .when().put("/association")
                .then().statusCode(403).body("code", equalTo("ASSOCIATION_SELECTION_REQUIRED"));
        given().auth().oauth2(token("president")).header(ASSOCIATION_HEADER, ASSOCIATION_B.toString())
                .when().get("/seasons")
                .then().statusCode(403).body("code", equalTo("ASSOCIATION_SELECTION_REQUIRED"));
        given().auth().oauth2(token("president")).header(ASSOCIATION_HEADER, ASSOCIATION_B.toString())
                .contentType(ContentType.JSON).body(Map.of("startYear", 2035))
                .when().post("/seasons")
                .then().statusCode(403).body("code", equalTo("ASSOCIATION_SELECTION_REQUIRED"));

        given().auth().oauth2(token("councillor")).when().get("/seasons")
                .then().statusCode(200).body("label", not(hasItem("2035/2036")));
        given().auth().oauth2(token("president")).when().get("/seasons")
                .then().statusCode(200).body("label", not(hasItem("2035/2036")));
        given().auth().oauth2(token("councillor")).when().get("/association")
                .then().statusCode(200).body("name", equalTo(nameOfB)).body("name", not(containsString("Intrusa")));
        given().auth().oauth2(token("president")).when().get("/association")
                .then().statusCode(200).body("name", not(containsString("Intrusa")));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Reads the identifiers of the seasons a user lists.
     *
     * @param user realm user
     * @return the identifiers
     */
    private Set<String> seasonIds(final String user) {
        final List<String> ids = given().auth().oauth2(token(user)).when().get("/seasons")
                .then().statusCode(200).extract().jsonPath().getList("id", String.class);
        return new HashSet<>(ids);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Posts a default season by start year.
     *
     * @param user      realm user
     * @param startYear start year
     * @return the response
     */
    private Response postSeason(final String user, final int startYear) {
        return postSeasonBody(user, Map.of("startYear", startYear));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Posts a season body.
     *
     * @param user realm user
     * @param body request body
     * @return the response
     */
    private Response postSeasonBody(final String user, final Map<String, Object> body) {
        return given().auth().oauth2(token(user)).contentType(ContentType.JSON).body(body).when().post("/seasons");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Sends an association update.
     *
     * @param user realm user
     * @param body update body
     * @return the response
     */
    private Response put(final String user, final Map<String, Object> body) {
        return given().auth().oauth2(token(user)).contentType(ContentType.JSON).body(body).when().put("/association");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Builds an update body that reproduces the values of an association read.
     *
     * @param read the response of a read
     * @return the equivalent update body
     */
    private static Map<String, Object> updateBody(final Response read) {
        final Map<String, Object> body = new HashMap<>();
        body.put("name", read.path("name"));
        body.put("taxCode", read.path("taxCode"));
        body.put("office", read.path("registeredOffice"));
        body.put("affiliationBody", read.path("affiliationBody"));
        body.put("affiliationCode", read.path("affiliationCode"));
        return body;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Copies an update body with another name.
     *
     * @param body the body
     * @param name the new name
     * @return the copy
     */
    private static Map<String, Object> withName(final Map<String, Object> body, final String name) {
        final Map<String, Object> copy = new HashMap<>(body);
        copy.put("name", name);
        return copy;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Deletes every season of associations A and B, natively and bound to the tenant.
     */
    private void deleteSeasons() {
        for (final UUID tenant : new UUID[]{ASSOCIATION_A, ASSOCIATION_B}) {
            TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew().call(() -> entityManager
                    .createNativeQuery("DELETE FROM users.season WHERE tenant_id = CAST(:t AS UUID)")
                    .setParameter("t", tenant.toString()).executeUpdate()));
        }
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
