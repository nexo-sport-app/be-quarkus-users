package it.nexoapp.usr;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile in which the vice-president realm user is the configured President, used to race two first
 * logins of the same identity. Association id and tax code are distinct from every other profile because the
 * tests share one database and the tax code is globally unique.
 */
public class ConcurrentBootstrapProfile implements QuarkusTestProfile {

    /** Association configured by this profile. */
    public static final String ASSOCIATION_ID = "a55d0000-0000-4000-8000-00000000000c";

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
                "usr.bootstrap.tax-code", "97000000012",
                "usr.bootstrap.president-email", "vice-president@nexo.test");
    }

}
