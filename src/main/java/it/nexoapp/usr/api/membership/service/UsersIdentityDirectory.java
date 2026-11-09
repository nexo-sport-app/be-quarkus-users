package it.nexoapp.usr.api.membership.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import it.nexoapp.common.security.IdentityDirectory;
import it.nexoapp.common.security.IdentityKey;
import it.nexoapp.common.security.MembershipRef;
import it.nexoapp.common.security.Role;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.api.membership.entity.IdentityLinkEntity;
import it.nexoapp.usr.api.membership.entity.MembershipRoleEntity;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * {@link IdentityDirectory} of the users service: resolves an external identity to its memberships through
 * {@code identity_link} (global table, read unscoped) and {@code membership_role} (tenant table, read inside the
 * tenant of each link). It never reads token claims other than issuer and subject (D-01, D-03 amended).
 */
@ApplicationScoped
@RequiredArgsConstructor
class UsersIdentityDirectory implements IdentityDirectory {

    /* - repository - */
    private final IdentityLinkRepository identityLinkRepository;
    private final MembershipRoleRepository membershipRoleRepository;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param key {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public List<MembershipRef> memberships(final IdentityKey key) {
        final List<IdentityLinkEntity> links = TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew()
                .call(() -> identityLinkRepository.findByIssuerAndSubject(key.issuer(), key.subject())));

        return links.stream()
                .map(link -> TenantContext.callAs(link.getTenantId(), () -> QuarkusTransaction.requiringNew()
                        .call(() -> toReference(link))))
                .toList();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Builds the membership reference of one link. Must run inside the tenant of the link and a transaction.
     *
     * @param link the identity link
     * @return the association, the member and the roles held
     */
    private MembershipRef toReference(final IdentityLinkEntity link) {
        final Set<Role> roles = EnumSet.noneOf(Role.class);
        for (final MembershipRoleEntity entity : membershipRoleRepository.findByMembership(link.getMembershipId())) {
            roles.add(entity.getRole());
        }
        return new MembershipRef(link.getTenantId(), link.getMembershipId(), roles);
    }

}
