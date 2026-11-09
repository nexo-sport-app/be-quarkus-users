package it.nexoapp.usr;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.http.ContentType;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.api.membership.service.UsersTestFixtures;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Actor attribution end to end with real tokens: the seeded association belongs to the system actor, the rows the
 * President's first login creates belong to the identity provider actor, and every write the President makes over
 * REST carries the President's member identifier. Association A is restored after the test and season start year
 * 2040 is reserved for it.
 */
@QuarkusTest
class AuditActorsEndToEndTest {

    /** Association configured for the test profile. */
    private static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000a";

    /** Actor of the association seeded at startup. */
    private static final String SYSTEM_ACTOR = "system:usr";

    /** Actor of the rows created by the President's first login (provisioning from the identity provider). */
    private static final String IDP_ACTOR = "idp:" + LinkedIdentityFixture.PRESIDENT_SUBJECT;

    /** Reserved season start year of this test. */
    private static final int SEASON_YEAR = 2040;

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
     * Starts from an association with no linked identity and no season of the reserved year.
     */
    @BeforeEach
    void prepare() {
        LinkedIdentityFixture.clean(entityManager, ASSOCIATION_ID);
        deleteReservedSeason();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Removes what the test created and puts association A back to its configured values.
     */
    @AfterEach
    void cleanUp() {
        try {
            deleteReservedSeason();
        } finally {
            fixtures.restoreTestAssociation();
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The association is created by the system actor, the bootstrap rows by the identity provider actor, and the
     * President's writes by the President's member identifier.
     */
    @Test
    void everyWriteIsAttributedToTheRightActor() {
        assertEquals(SYSTEM_ACTOR, text("SELECT created_by FROM users.association WHERE id = CAST(:a AS UUID)"));

        final String memberId = given().auth().oauth2(token()).when().get("/me")
                .then().statusCode(200).body("linked", equalTo(true))
                .extract().path("memberId");

        assertEquals(IDP_ACTOR, text("SELECT created_by FROM users.membership WHERE id = CAST('" + memberId + "' AS UUID)"));
        assertEquals(IDP_ACTOR, text("SELECT created_by FROM users.membership_role WHERE membership_id = CAST('"
                + memberId + "' AS UUID) AND role = 'PRESIDENT'"));
        assertEquals(IDP_ACTOR, text("SELECT created_by FROM users.identity_link WHERE membership_id = CAST('"
                + memberId + "' AS UUID)"));

        final Map<String, Object> office = Map.of("street", "Via Audit 1", "zipCode", "20100",
                "city", "Milano", "province", "MI", "state", "IT");
        final Map<String, Object> update = new HashMap<>();
        update.put("name", "ASD Audit");
        update.put("taxCode", "97000000010");
        update.put("office", office);
        given().auth().oauth2(token()).contentType(ContentType.JSON).body(update)
                .when().put("/association")
                .then().statusCode(200).body("name", equalTo("ASD Audit"));
        assertEquals(memberId, text("SELECT updated_by FROM users.association WHERE id = CAST(:a AS UUID)"));
        assertEquals(SYSTEM_ACTOR, text("SELECT created_by FROM users.association WHERE id = CAST(:a AS UUID)"));

        given().auth().oauth2(token()).contentType(ContentType.JSON).body(Map.of("startYear", SEASON_YEAR))
                .when().post("/seasons")
                .then().statusCode(201).body("label", equalTo("2040/2041"));
        assertEquals(memberId, text("SELECT created_by FROM users.season WHERE tenant_id = CAST(:a AS UUID) "
                + "AND start_date = DATE '2040-09-01'"));
        assertEquals(memberId, text("SELECT updated_by FROM users.season WHERE tenant_id = CAST(:a AS UUID) "
                + "AND start_date = DATE '2040-09-01'"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Removes the season of the reserved start year of association A.
     */
    private void deleteReservedSeason() {
        unscoped(() -> entityManager.createNativeQuery("DELETE FROM users.season WHERE tenant_id = CAST(:a AS UUID) "
                        + "AND start_date = DATE '2040-09-01'")
                .setParameter("a", ASSOCIATION_ID).executeUpdate());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Reads one text value with a native query, unscoped and in its own transaction (native SQL is never
     * tenant-filtered). The named parameter {@code :a} is bound to association A.
     *
     * @param sql query returning a single text column and exactly one row
     * @return the value
     */
    private String text(final String sql) {
        return unscoped(() -> {
            final var query = entityManager.createNativeQuery(sql);
            if (sql.contains(":a")) {
                query.setParameter("a", ASSOCIATION_ID);
            }
            return String.valueOf(query.getSingleResult());
        });
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Runs a database task unscoped and in its own transaction.
     *
     * @param task the task
     * @param <T>  result type
     * @return the task result
     */
    private <T> T unscoped(final Supplier<T> task) {
        return TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew().call(task::get));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Gets an access token of the realm user {@code president}.
     *
     * @return the access token
     */
    private String token() {
        return keycloak.getAccessToken("president", "password", "nexo-dev");
    }

}
