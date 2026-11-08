package it.nexoapp.usr.api.membership.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.api.membership.entity.MembershipEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Persistence of {@link MembershipEntity}. Package-private: only the membership services use it.
 * Every query runs inside the tenant of the active scope, so it only ever sees the memberships of that tenant.
 */
@ApplicationScoped
class MembershipRepository implements PanacheRepositoryBase<MembershipEntity, UUID> {

}
