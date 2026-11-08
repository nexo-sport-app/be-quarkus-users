package it.nexoapp.usr.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;
import java.util.UUID;

/**
 * Users microservice configuration, mapped from configuration properties with the {@code usr} prefix.
 * <p>
 * The only content is the optional bootstrap block ({@code usr.bootstrap.*}): the association that exists
 * only in configuration and is created once at startup. It seeds, it never acts as a runtime fallback.
 */
@StaticInitSafe
@ConfigMapping(prefix = "usr")
public interface UsersConfig {

    /**
     * Association to seed at startup (properties {@code usr.bootstrap.*}).
     * Optional: when no key of the block is set the service starts without seeding anything.
     * When at least one key is set, every required key of the block must be set.
     */
    Optional<Bootstrap> bootstrap();

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Association created once at startup, inside its own tenant, attributed to the system actor.
     */
    interface Bootstrap {

        /**
         * Identifier of the association, which is also its tenant (property {@code usr.bootstrap.association-id}).
         * Must be configured.
         */
        UUID associationId();

        /**
         * Name of the association (property {@code usr.bootstrap.name}). Must be configured.
         */
        String name();

        /**
         * Tax code of the association, globally unique (property {@code usr.bootstrap.tax-code}).
         * Must be configured.
         */
        String taxCode();

        /**
         * Registered office of the association (properties {@code usr.bootstrap.office.*}). Must be configured.
         */
        Office office();

        /**
         * Sports body the association is affiliated with (property {@code usr.bootstrap.affiliation-body}).
         * Optional.
         */
        Optional<String> affiliationBody();

        /**
         * Affiliation code issued by the sports body (property {@code usr.bootstrap.affiliation-code}).
         * Optional.
         */
        Optional<String> affiliationCode();

        /**
         * Verified email of the person who becomes the President at first login
         * (property {@code usr.bootstrap.president-email}). Must be configured.
         */
        String presidentEmail();

    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Registered office of the bootstrap association.
     */
    interface Office {

        /**
         * Street and number (property {@code usr.bootstrap.office.street}). Must be configured.
         */
        String street();

        /**
         * Postal code (property {@code usr.bootstrap.office.zip-code}). Must be configured.
         */
        String zipCode();

        /**
         * City (property {@code usr.bootstrap.office.city}). Must be configured.
         */
        String city();

        /**
         * Province code (property {@code usr.bootstrap.office.province}). Must be configured.
         */
        String province();

        /**
         * Country code (property {@code usr.bootstrap.office.state}). Must be configured.
         */
        String state();

    }

}
