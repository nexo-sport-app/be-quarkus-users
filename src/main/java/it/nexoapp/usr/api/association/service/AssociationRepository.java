package it.nexoapp.usr.api.association.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.api.association.entity.AssociationEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Persistence of {@link AssociationEntity}. Package-private: only the association services use it.
 * Every query runs inside the tenant of the active scope, so it only ever sees the association of that tenant.
 */
@ApplicationScoped
class AssociationRepository implements PanacheRepositoryBase<AssociationEntity, UUID> {

}
