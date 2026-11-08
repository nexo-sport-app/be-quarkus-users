package it.nexoapp.usr.api.association.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import it.nexoapp.common.configuration.AppInfo;
import it.nexoapp.common.context.ExecutionScope;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.api.association.entity.AssociationEntity;
import it.nexoapp.usr.config.UsersConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Creates, at startup, the association configured under {@code usr.bootstrap}. The association exists only in
 * configuration and seed data: this class creates the row when it is absent and never updates an existing one,
 * so configuration can never overwrite live data. The row is written inside its own tenant scope, attributed
 * to the system actor.
 */
@ApplicationScoped
@RequiredArgsConstructor
class AssociationSeeder {

    /* - logger - */
    private static final Logger logger = LoggerFactory.getLogger(AssociationSeeder.class);

    /* - config - */
    private final UsersConfig config;
    private final AppInfo appInfo;

    /* - repository - */
    private final AssociationRepository repository;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Seeds the configured association when the application starts.
     *
     * @param event startup event, not used
     */
    void onStart(@Observes final StartupEvent event) {
        seed();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates the configured association if it does not exist yet. Never updates an existing row.
     * Runs with the system actor and inside the tenant of the association, with a new transaction opened
     * after the scopes.
     *
     * @return true when the association was created, false when it already existed or nothing is configured
     */
    boolean seed() {
        final Optional<UsersConfig.Bootstrap> bootstrap = config.bootstrap();
        if (bootstrap.isEmpty()) {
            logger.info("No association bootstrap configured, nothing to seed");
            return false;
        }

        final UsersConfig.Bootstrap source = bootstrap.get();
        final UUID id = source.associationId();
        final String actor = ExecutionScope.systemActor(appInfo.code());

        return ExecutionScope.callAs(actor, () -> TenantContext.callAs(id,
                () -> QuarkusTransaction.requiringNew().call(() -> createIfAbsent(source))));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Persists the association built from configuration when no row has its identifier.
     * Must run inside the tenant scope of the association and inside a transaction.
     *
     * @param source configured association
     * @return true when the row was created
     */
    private boolean createIfAbsent(final UsersConfig.Bootstrap source) {
        final UUID id = source.associationId();
        if (repository.findById(id) != null) {
            logger.info("Association {} already exists, configuration left untouched", id);
            return false;
        }

        final UsersConfig.Office office = source.office();
        final AssociationEntity association = AssociationEntity.builder()
                .id(id)
                .name(source.name())
                .taxCode(source.taxCode())
                .officeStreet(office.street())
                .officeZipCode(office.zipCode())
                .officeCity(office.city())
                .officeProvince(office.province())
                .officeState(office.state())
                .affiliationBody(source.affiliationBody().orElse(null))
                .affiliationCode(source.affiliationCode().orElse(null))
                .build();
        repository.persist(association);
        logger.info("Association {} created from configuration", id);
        return true;
    }

}
