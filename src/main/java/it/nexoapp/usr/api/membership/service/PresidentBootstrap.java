package it.nexoapp.usr.api.membership.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import it.nexoapp.common.context.ExecutionScope;
import it.nexoapp.common.security.IdentityBootstrap;
import it.nexoapp.common.security.IdentityKey;
import it.nexoapp.common.security.MembershipRef;
import it.nexoapp.common.security.Role;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.api.association.entity.AssociationEntity;
import it.nexoapp.usr.api.membership.entity.IdentityLinkEntity;
import it.nexoapp.usr.api.membership.entity.MembershipEntity;
import it.nexoapp.usr.api.membership.entity.MembershipEntity.MembershipStatus;
import it.nexoapp.usr.api.membership.entity.MembershipRoleEntity;
import it.nexoapp.usr.config.UsersConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@link IdentityBootstrap} of the users service: links the configured President on the first login (D-03,
 * amended 2026-11-08). It is called by the augmentor only while the identity is unlinked and only with an
 * IdP-verified email, which is compared to {@code usr.bootstrap.president-email} ignoring case. The email is
 * never used as an identity key and never read once the identity is linked: later requests resolve through
 * {@code identity_link} alone. At most one identity is ever linked as President of the association.
 */
@ApplicationScoped
@RequiredArgsConstructor
class PresidentBootstrap implements IdentityBootstrap {

    /* - logger - */
    private static final Logger logger = LoggerFactory.getLogger(PresidentBootstrap.class);

    /* - config - */
    private final UsersConfig config;

    /* - persistence - */
    private final EntityManager entityManager;

    /* - repository - */
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final IdentityLinkRepository identityLinkRepository;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param key           {@inheritDoc}
     * @param verifiedEmail {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Optional<MembershipRef> link(final IdentityKey key, final String verifiedEmail) {
        final Optional<UsersConfig.Bootstrap> bootstrap = config.bootstrap();
        if (bootstrap.isEmpty() || verifiedEmail == null
                || !bootstrap.get().presidentEmail().equalsIgnoreCase(verifiedEmail.strip())) {
            return Optional.empty();
        }
        final UUID associationId = bootstrap.get().associationId();

        return ExecutionScope.callAs(ExecutionScope.idpActor(key.subject()), () -> TenantContext.callAs(associationId,
                () -> QuarkusTransaction.requiringNew().call(() -> linkInTransaction(key, associationId))));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Links the identity as President under a lock of the association row, so that concurrent first logins
     * serialize. Must run inside the tenant scope of the association and one transaction.
     *
     * @param key           the identity to link
     * @param associationId the configured association
     * @return the membership found or created, empty when the association is missing or already has a linked
     * President
     */
    private Optional<MembershipRef> linkInTransaction(final IdentityKey key, final UUID associationId) {
        final AssociationEntity association = entityManager.find(AssociationEntity.class, associationId,
                LockModeType.PESSIMISTIC_WRITE);
        if (association == null) {
            return Optional.empty();
        }

        // re-read under the lock: a concurrent request may have linked this very identity already
        final List<IdentityLinkEntity> existing = identityLinkRepository
                .findByIssuerAndSubject(key.issuer(), key.subject());
        for (final IdentityLinkEntity link : existing) {
            if (associationId.equals(link.getTenantId())) {
                return Optional.of(new MembershipRef(associationId, link.getMembershipId(), rolesOf(link)));
            }
        }

        final List<UUID> presidents = membershipRoleRepository.findByRole(Role.PRESIDENT).stream()
                .map(MembershipRoleEntity::getMembershipId)
                .toList();
        if (identityLinkRepository.existsForAnyMembership(presidents)) {
            logger.debug("President of association {} is already linked", associationId);
            return Optional.empty();
        }

        final MembershipEntity membership = MembershipEntity.builder().status(MembershipStatus.ACTIVE).build();
        membershipRepository.persist(membership);
        membershipRoleRepository.persist(MembershipRoleEntity.builder()
                .membershipId(membership.getId())
                .role(Role.PRESIDENT)
                .build());
        identityLinkRepository.persist(IdentityLinkEntity.builder()
                .issuer(key.issuer())
                .subject(key.subject())
                .membershipId(membership.getId())
                .tenantId(associationId)
                .linkedAt(OffsetDateTime.now())
                .build());

        logger.info("President bootstrap linked for association {}", associationId);
        return Optional.of(new MembershipRef(associationId, membership.getId(), Set.of(Role.PRESIDENT)));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Reads the roles of the membership of a link. Must run inside the tenant scope and a transaction.
     *
     * @param link the identity link
     * @return the roles held
     */
    private Set<Role> rolesOf(final IdentityLinkEntity link) {
        return Set.copyOf(membershipRoleRepository.findByMembership(link.getMembershipId()).stream()
                .map(MembershipRoleEntity::getRole)
                .toList());
    }

}
