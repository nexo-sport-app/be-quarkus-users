package it.nexoapp.usr.api.membership.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.common.security.Role;
import it.nexoapp.usr.api.membership.entity.MembershipRoleEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Persistence of {@link MembershipRoleEntity}. Package-private: only the membership services use it.
 * Every query runs inside the tenant of the active scope, so it only ever sees the roles of that tenant.
 */
@ApplicationScoped
class MembershipRoleRepository implements PanacheRepositoryBase<MembershipRoleEntity, UUID> {

    /**
     * Finds the roles held by one membership.
     *
     * @param membershipId identifier of the membership
     * @return the role assignments of the membership, empty when it holds none
     */
    List<MembershipRoleEntity> findByMembership(final UUID membershipId) {
        return list("membershipId", membershipId);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Finds every assignment of one role in the active tenant.
     *
     * @param role the role to look for
     * @return the assignments of the role, empty when nobody holds it
     */
    List<MembershipRoleEntity> findByRole(final Role role) {
        return list("role", role);
    }

}
