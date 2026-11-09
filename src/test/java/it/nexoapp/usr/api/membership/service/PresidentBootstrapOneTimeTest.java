package it.nexoapp.usr.api.membership.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import it.nexoapp.common.security.IdentityKey;
import it.nexoapp.usr.LinkedIdentityFixture;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bootstrap is a first-login hook, not a per-request check (D-03 amended 2026-11-08): once the President is
 * linked the email claim is never read again, and no second identity can become President of the association.
 * The test lives in the package of the bootstrap to reach the package-private bean.
 */
@QuarkusTest
class PresidentBootstrapOneTimeTest {

    /** Association configured for the default test profile. */
    private static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000a";

    /** Configured President email, in the realm. */
    private static final String PRESIDENT_EMAIL = "president@nexo.test";

    /* - keycloak - */
    private final KeycloakTestClient keycloak = new KeycloakTestClient();

    /* - spy - */
    @InjectSpy
    PresidentBootstrap bootstrap;

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
        LinkedIdentityFixture.clean(entityManager, ASSOCIATION_ID);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The first login consults the bootstrap once; the next calls resolve through the identity link and never
     * invoke it again.
     */
    @Test
    void bootstrapIsNotCalledAfterLinking() {
        final String token = keycloak.getAccessToken("president", "password", "nexo-dev");

        final String memberId = given().auth().oauth2(token)
                .when().get("/me")
                .then().statusCode(200).body("linked", equalTo(true))
                .extract().path("memberId");
        Mockito.verify(bootstrap, Mockito.times(1)).link(Mockito.any(), Mockito.any());

        Mockito.clearInvocations(bootstrap);
        for (int i = 0; i < 3; i++) {
            given().auth().oauth2(token)
                    .when().get("/me")
                    .then().statusCode(200).body("memberId", equalTo(memberId));
        }

        Mockito.verifyNoInteractions(bootstrap);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * With the President linked, another subject presenting the President's email is refused: there is never a
     * second President, and nothing is created for that subject.
     */
    @Test
    void noSecondPresidentIsLinked() {
        given().auth().oauth2(keycloak.getAccessToken("president", "password", "nexo-dev"))
                .when().get("/me").then().statusCode(200).body("linked", equalTo(true));

        final IdentityKey impostor = new IdentityKey(issuer, "impostor-subject");
        assertTrue(bootstrap.link(impostor, PRESIDENT_EMAIL).isEmpty(), "a second President must not be linked");

        assertEquals(0, LinkedIdentityFixture.linksOfSubject(entityManager, impostor.subject()));
        assertEquals(1, LinkedIdentityFixture.count(entityManager, "identity_link", ASSOCIATION_ID));
        assertEquals(1, LinkedIdentityFixture.countRole(entityManager, "PRESIDENT", ASSOCIATION_ID));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The email comparison ignores case and surrounding blanks, and a different email links nobody.
     */
    @Test
    void emailMatchIgnoresCaseOnly() {
        final IdentityKey other = new IdentityKey(issuer, "other-subject");
        assertTrue(bootstrap.link(other, "somebody.else@nexo.test").isEmpty());
        assertEquals(0, LinkedIdentityFixture.count(entityManager, "identity_link", ASSOCIATION_ID));

        final IdentityKey president = new IdentityKey(issuer, "case-subject");
        assertTrue(bootstrap.link(president, "  President@NEXO.test ").isPresent());
        assertEquals(1, LinkedIdentityFixture.count(entityManager, "identity_link", ASSOCIATION_ID));
    }

}
