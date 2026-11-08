package it.nexoapp.usr.api.association.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.api.association.entity.AssociationEntity;
import it.nexoapp.usr.config.UsersConfig;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Proves that the association configured under {@code usr.bootstrap} is seeded exactly once, inside its own
 * tenant, attributed to the system actor, and that configuration never overwrites live data.
 */
@QuarkusTest
class AssociationSeederTest {

    /* - seeder - */
    @Inject
    AssociationSeeder seeder;

    /* - repository - */
    @Inject
    AssociationRepository repository;

    /* - configuration - */
    @Inject
    UsersConfig config;

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The row of the configured association exists with the configured values, belongs to its own tenant
     * and is attributed to {@code system:usr}.
     */
    @Test
    void seedsTheConfiguredAssociationInItsOwnTenant() {
        final UsersConfig.Bootstrap bootstrap = config.bootstrap().orElseThrow();
        final UUID id = bootstrap.associationId();

        final AssociationEntity row = inTenant(id, () -> repository.findById(id));

        assertNotNull(row, "the configured association must exist after startup");
        assertEquals(bootstrap.name(), row.getName());
        assertEquals(bootstrap.taxCode(), row.getTaxCode());
        assertEquals(bootstrap.office().street(), row.getOfficeStreet());
        assertEquals(bootstrap.office().zipCode(), row.getOfficeZipCode());
        assertEquals(bootstrap.office().city(), row.getOfficeCity());
        assertEquals(bootstrap.office().province(), row.getOfficeProvince());
        assertEquals(bootstrap.office().state(), row.getOfficeState());
        assertEquals("system:usr", row.getCreatedBy());

        final Object[] raw = inTenant(id, () -> (Object[]) entityManager
                .createNativeQuery("SELECT CAST(tenant_id AS TEXT), created_by FROM users.association WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult());
        assertEquals(id.toString(), raw[0], "the tenant of the association is the association itself");
        assertEquals("system:usr", raw[1]);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Runs a call inside the tenant scope and, inside it, a new transaction (scope before transaction).
     *
     * @param tenant tenant of the scope
     * @param call   call to run
     * @param <T>    type of the result
     * @return the value returned by the call
     */
    private static <T> T inTenant(final UUID tenant, final java.util.function.Supplier<T> call) {
        return TenantContext.callAs(tenant, () -> QuarkusTransaction.requiringNew().call(call::get));
    }

}
