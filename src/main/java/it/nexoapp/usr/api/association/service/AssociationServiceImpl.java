package it.nexoapp.usr.api.association.service;

import it.nexoapp.common.context.RequestContext;
import it.nexoapp.common.hibernate.exception.EntityNotFoundException;
import it.nexoapp.usr.api.association.dto.AssociationResponse;
import it.nexoapp.usr.api.association.dto.RegisteredOffice;
import it.nexoapp.usr.api.association.entity.AssociationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Default {@link AssociationService}. The association is loaded through the tenant-filtered repository by the
 * association id of the request: no explicit tenant predicate and no configured identifier are involved.
 */
@ApplicationScoped
@RequiredArgsConstructor
class AssociationServiceImpl implements AssociationService {

    /* - repository - */
    private final AssociationRepository repository;

    /* - context - */
    private final RequestContext requestContext;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public AssociationResponse getCurrent() {
        final AssociationEntity entity = repository.findById(requestContext.getAssociationId());
        if (entity == null) {
            throw new EntityNotFoundException("association");
        }
        return toResponse(entity);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Maps the entity to the public profile, leaving out audit columns and the tenant column.
     *
     * @param entity the association
     * @return the profile
     */
    private AssociationResponse toResponse(final AssociationEntity entity) {
        return new AssociationResponse(
                entity.getId(),
                entity.getName(),
                entity.getTaxCode(),
                new RegisteredOffice(
                        entity.getOfficeStreet(),
                        entity.getOfficeZipCode(),
                        entity.getOfficeCity(),
                        entity.getOfficeProvince(),
                        entity.getOfficeState()),
                entity.getAffiliationBody(),
                entity.getAffiliationCode());
    }

}
