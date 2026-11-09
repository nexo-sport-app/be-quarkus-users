package it.nexoapp.usr.api.association;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.http.ContentType;
import it.nexoapp.usr.LinkedIdentityFixture;
import it.nexoapp.usr.api.membership.service.UsersTestFixtures;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

/**
 * Invalid association updates are refused with one 400 Fault listing every violation, and the size and
 * encoding limits sit exactly where the database columns do (FND-05, ONB-01).
 */
@QuarkusTest
class AssociationValidationTest {

    /** Association configured for the test profile. */
    private static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000a";

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
     * Starts with the President of association A linked by a first login.
     */
    @BeforeEach
    void prepare() {
        LinkedIdentityFixture.clean(entityManager, ASSOCIATION_ID);
        given().auth().oauth2(token()).when().get("/me").then().statusCode(200).body("linked", equalTo(true));
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
     * A blank name, a malformed tax code and a missing office are all reported in one Fault, sorted by field.
     */
    @Test
    void everyViolationIsReportedInOneFault() {
        given().auth().oauth2(token()).contentType(ContentType.JSON)
                .body(Map.of("name", "", "taxCode", "ABC"))
                .when().put("/association")
                .then().statusCode(400)
                .body("code", equalTo("nexo.validation"))
                .body("violations.size()", greaterThanOrEqualTo(3))
                .body("violations.field", hasItem("name"))
                .body("violations.field", hasItem("office"))
                .body("violations.field", hasItem("taxCode"))
                .body("violations.field", contains("name", "office", "taxCode"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A name of exactly 120 characters is accepted and one of 121 is rejected with a Size violation.
     */
    @Test
    void nameLengthLimitIsExact() {
        final String max = "a".repeat(120);

        given().auth().oauth2(token()).contentType(ContentType.JSON)
                .body(body(max, "20100"))
                .when().put("/association")
                .then().statusCode(200).body("name", equalTo(max));

        given().auth().oauth2(token()).contentType(ContentType.JSON)
                .body(body(max + "a", "20100"))
                .when().put("/association")
                .then().statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", equalTo("name"))
                .body("violations[0].code", equalTo("Size"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A postal code that is not five digits is a violation on the nested field.
     */
    @Test
    void shortZipCodeIsRejectedOnTheNestedField() {
        given().auth().oauth2(token()).contentType(ContentType.JSON)
                .body(body("ASD Nexo Test", "1234"))
                .when().put("/association")
                .then().statusCode(400)
                .body("code", equalTo("nexo.validation"))
                .body("violations", hasSize(1))
                .body("violations[0].field", equalTo("office.zipCode"))
                .body("violations[0].code", equalTo("Pattern"));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Gets an access token of the President.
     *
     * @return the access token
     */
    private String token() {
        return keycloak.getAccessToken("president", "password", "nexo-dev");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Builds an otherwise valid update body.
     *
     * @param name    association name
     * @param zipCode postal code
     * @return the request body as a map
     */
    private static Map<String, Object> body(final String name, final String zipCode) {
        return Map.of(
                "name", name,
                "taxCode", "97000000010",
                "office", Map.of("street", "Via Test 10", "zipCode", zipCode,
                        "city", "Milano", "province", "MI", "state", "IT"));
    }

}
