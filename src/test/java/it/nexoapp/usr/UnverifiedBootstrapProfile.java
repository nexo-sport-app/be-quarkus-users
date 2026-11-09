package it.nexoapp.usr;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile in which the configured President email is the one of the realm user whose email the IdP has NOT
 * verified. The association, its id and its tax code are distinct from every other profile because the tests
 * share one database and the tax code is globally unique.
 */
public class UnverifiedBootstrapProfile implements QuarkusTestProfile {

    /** Association configured by this profile. */
    public static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000b";

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "usr.bootstrap.association-id", ASSOCIATION_ID,
                "usr.bootstrap.tax-code", "97000000011",
                "usr.bootstrap.president-email", "unverified@nexo.test");
    }

}
