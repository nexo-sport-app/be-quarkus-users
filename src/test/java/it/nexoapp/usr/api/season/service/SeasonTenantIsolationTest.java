package it.nexoapp.usr.api.season.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.common.testing.TenantIsolationHarness;
import it.nexoapp.usr.api.season.entity.SeasonEntity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Proves the shared two-tenant harness against the season table. The unique key is the start date: every key maps
 * deterministically to one calendar day, so the same key collides only inside one tenant. Rows are written through
 * the repository directly, which bypasses the overlap rule of the service.
 */
@QuarkusTest
class SeasonTenantIsolationTest extends TenantIsolationHarness {

    /** Number of distinct days a key can map to. */
    private static final int DAY_SPAN = 36_500;

    /* - repository - */
    @Inject
    SeasonRepository repository;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Removes the rows of both harness tenants after each test.
     */
    @AfterEach
    void cleanUp() {
        for (final UUID tenant : new UUID[]{TENANT_A, TENANT_B}) {
            TenantContext.runAs(tenant, () -> QuarkusTransaction.requiringNew().run(() -> repository.deleteAll()));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected UUID createAs(final UUID tenant, final String uniqueKey) {
        final LocalDate start = LocalDate.of(1900, 1, 1).plusDays(Math.floorMod(uniqueKey.hashCode(), DAY_SPAN));
        return TenantContext.callAs(tenant, () -> QuarkusTransaction.requiringNew().call(() -> {
            final SeasonEntity season = SeasonEntity.builder()
                    .label(start.getYear() + "/" + start.plusDays(1).getYear())
                    .startDate(start)
                    .endDate(start.plusDays(1))
                    .build();
            repository.persist(season);
            repository.flush();
            return season.getId();
        }));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean existsAs(final UUID tenant, final UUID id) {
        return TenantContext.callAs(tenant, () -> QuarkusTransaction.requiringNew().call(() ->
                repository.findById(id) != null));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected long countAs(final UUID tenant) {
        return TenantContext.callAs(tenant, () -> QuarkusTransaction.requiringNew().call(() -> repository.count()));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected long countOnCurrentThread() {
        return QuarkusTransaction.requiringNew().call(() -> repository.count());
    }

}
