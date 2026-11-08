package it.nexoapp.usr.api.association.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import it.nexoapp.common.context.ExecutionScope;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.api.association.entity.AssociationEntity;
import it.nexoapp.usr.config.UsersConfig;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
     * Re-running the seeding step leaves exactly one row for the configured identifier.
     */
    @Test
    void reRunningTheSeedingStepKeepsOneRow() {
        final UUID id = config.bootstrap().orElseThrow().associationId();

        assertFalse(seeder.seed(), "nothing is created when the association already exists");
        assertFalse(seeder.seed());

        assertEquals(1L, inTenant(id, () -> repository.count("id", id)));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A changed name survives a re-run of the seeding step: configuration never overwrites live data.
     * The configured name is restored afterwards because later tests assert it.
     */
    @Test
    void configurationNeverOverwritesALiveRow() {
        final UsersConfig.Bootstrap bootstrap = config.bootstrap().orElseThrow();
        final UUID id = bootstrap.associationId();
        try {
            ExecutionScope.runAs("idp:changed-by-test", () -> inTenant(id, () -> {
                repository.findById(id).setName("Renamed by the board");
                return null;
            }));

            seeder.seed();

            final AssociationEntity row = inTenant(id, () -> repository.findById(id));
            assertEquals("Renamed by the board", row.getName());
            assertEquals("idp:changed-by-test", row.getUpdatedBy());
        } finally {
            ExecutionScope.runAs("system:usr", () -> inTenant(id, () -> {
                repository.findById(id).setName(bootstrap.name());
                return null;
            }));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The seeding step works on a bare executor thread, with no HTTP request, and still attributes the system actor.
     */
    @Test
    void seedsWithoutAnHttpRequest() throws ExecutionException, InterruptedException {
        final UUID id = config.bootstrap().orElseThrow().associationId();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            final String createdBy = executor.submit(() -> {
                seeder.seed();
                return inTenant(id, () -> repository.findById(id).getCreatedBy());
            }).get();

            assertEquals("system:usr", createdBy);
        }
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
