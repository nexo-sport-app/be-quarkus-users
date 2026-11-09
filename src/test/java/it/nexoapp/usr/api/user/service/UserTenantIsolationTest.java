package it.nexoapp.usr.api.user.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.common.testing.TenantIsolationHarness;
import it.nexoapp.usr.api.user.entity.UserEntity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Proves the shared two-tenant harness against the user table. The unique key is the tax code, which is unique
 * within one association only: the same code is accepted in two associations and refused twice in one.
 */
@QuarkusTest
class UserTenantIsolationTest extends TenantIsolationHarness {

    /* - repository - */
    @Inject
    UserRepository repository;

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
        return TenantContext.callAs(tenant, () -> QuarkusTransaction.requiringNew().call(() -> {
            final UserEntity user = UserEntity.builder()
                    .firstName("Mario")
                    .lastName("Rossi")
                    .gender(UserEntity.Gender.M)
                    .taxCode(uniqueKey)
                    .nationality("IT")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .placeOfBirth("H501")
                    .build();
            repository.persist(user);
            repository.flush();
            return user.getId();
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
