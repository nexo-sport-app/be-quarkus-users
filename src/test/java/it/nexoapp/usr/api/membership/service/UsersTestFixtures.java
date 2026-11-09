package it.nexoapp.usr.api.membership.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import it.nexoapp.common.context.ExecutionScope;
import it.nexoapp.common.security.Role;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.api.association.entity.AssociationEntity;
import it.nexoapp.usr.api.membership.entity.IdentityLinkEntity;
import it.nexoapp.usr.api.membership.entity.MembershipEntity;
import it.nexoapp.usr.api.membership.entity.MembershipEntity.MembershipStatus;
import it.nexoapp.usr.api.membership.entity.MembershipRoleEntity;
import it.nexoapp.usr.config.UsersConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Test fixtures over the association and membership tables. It lives in the package of the membership
 * repositories, which are package-private, and writes inside the proper tenant scope with the system actor.
 */
@ApplicationScoped
public class UsersTestFixtures {

    /** Actor of the fixture writes. */
    private static final String ACTOR = ExecutionScope.systemActor("usr");

    /* - configuration - */
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String issuer;

    @Inject
    UsersConfig config;

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    /* - repository - */
    @Inject
    MembershipRepository membershipRepository;

    @Inject
    MembershipRoleRepository membershipRoleRepository;

    @Inject
    IdentityLinkRepository identityLinkRepository;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Links a token subject to a membership of an association holding one role. Idempotent: when the subject is
     * already linked to that association, the existing member identifier is returned and nothing is written.
     *
     * @param subject       token subject of the realm user
     * @param associationId association to join
     * @param role          the role the new membership holds
     * @return the identifier of the membership of the subject
     */
    public UUID linkMember(final String subject, final UUID associationId, final Role role) {
        final List<IdentityLinkEntity> links = TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew()
                .call(() -> identityLinkRepository.findByIssuerAndSubject(issuer, subject)));
        for (final IdentityLinkEntity link : links) {
            if (associationId.equals(link.getTenantId())) {
                return link.getMembershipId();
            }
        }

        return ExecutionScope.callAs(ACTOR, () -> TenantContext.callAs(associationId,
                () -> QuarkusTransaction.requiringNew().call(() -> {
                    final MembershipEntity membership = MembershipEntity.builder().status(MembershipStatus.ACTIVE).build();
                    membershipRepository.persist(membership);
                    membershipRoleRepository.persist(MembershipRoleEntity.builder()
                            .membershipId(membership.getId())
                            .role(role)
                            .build());
                    identityLinkRepository.persist(IdentityLinkEntity.builder()
                            .issuer(issuer)
                            .subject(subject)
                            .membershipId(membership.getId())
                            .tenantId(associationId)
                            .linkedAt(OffsetDateTime.now())
                            .build());
                    return membership.getId();
                })));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates an association with the given identifier, name and tax code inside its own tenant, unless it
     * already exists.
     *
     * @param id      identifier of the association, which is also its tenant
     * @param name    name of the association
     * @param taxCode tax code of the association
     */
    public void createAssociation(final UUID id, final String name, final String taxCode) {
        ExecutionScope.runAs(ACTOR, () -> TenantContext.runAs(id, () -> QuarkusTransaction.requiringNew().run(() -> {
            if (entityManager.find(AssociationEntity.class, id) != null) {
                return;
            }
            entityManager.persist(AssociationEntity.builder()
                    .id(id)
                    .name(name)
                    .taxCode(taxCode)
                    .officeStreet("Via Fixture 1")
                    .officeZipCode("00100")
                    .officeCity("Roma")
                    .officeProvince("RM")
                    .officeState("IT")
                    .build());
        })));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Writes the configured values of the test association back, undoing any update made by a test.
     */
    public void restoreTestAssociation() {
        final UsersConfig.Bootstrap source = config.bootstrap().orElseThrow();
        final UsersConfig.Office office = source.office();
        ExecutionScope.runAs(ACTOR, () -> TenantContext.runAs(source.associationId(),
                () -> QuarkusTransaction.requiringNew().run(() -> {
                    final AssociationEntity entity = entityManager.find(AssociationEntity.class, source.associationId());
                    entity.setName(source.name());
                    entity.setTaxCode(source.taxCode());
                    entity.setOfficeStreet(office.street());
                    entity.setOfficeZipCode(office.zipCode());
                    entity.setOfficeCity(office.city());
                    entity.setOfficeProvince(office.province());
                    entity.setOfficeState(office.state());
                    entity.setAffiliationBody(source.affiliationBody().orElse(null));
                    entity.setAffiliationCode(source.affiliationCode().orElse(null));
                })));
    }

}
