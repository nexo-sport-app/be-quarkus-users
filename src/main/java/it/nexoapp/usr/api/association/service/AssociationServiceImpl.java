package it.nexoapp.usr.api.association.service;

import it.nexoapp.common.context.RequestContext;
import it.nexoapp.common.hibernate.exception.EntityNotFoundException;
import it.nexoapp.usr.api.association.dto.AssociationResponse;
import it.nexoapp.usr.api.association.dto.AssociationUpdateRequest;
import it.nexoapp.usr.api.association.dto.RegisteredOfficeRequest;
import it.nexoapp.usr.api.association.dto.RegisteredOffice;
import it.nexoapp.usr.api.association.entity.AssociationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

/**
 * Default {@link AssociationService}. The association is loaded through the tenant-filtered repository by the
 * association id of the request: no explicit tenant predicate and no configured identifier are involved.
 */
@ApplicationScoped
@RequiredArgsConstructor
class AssociationServiceImpl implements AssociationService {

    /* - constants - */
    /** Name of the unique constraint on the tax code. */
    private static final String TAX_CODE_CONSTRAINT = "association_uq";

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
     * {@inheritDoc}
     *
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    @Transactional
    public AssociationResponse update(final AssociationUpdateRequest request) {
        final AssociationEntity entity = repository.findById(requestContext.getAssociationId());
        if (entity == null) {
            throw new EntityNotFoundException("association");
        }

        final RegisteredOfficeRequest office = request.office();
        entity.setName(request.name());
        entity.setTaxCode(request.taxCode());
        entity.setOfficeStreet(office.street());
        entity.setOfficeZipCode(office.zipCode());
        entity.setOfficeCity(office.city());
        entity.setOfficeProvince(office.province());
        entity.setOfficeState(office.state());
        entity.setAffiliationBody(request.affiliationBody());
        entity.setAffiliationCode(request.affiliationCode());

        try {
            // flush now: the unique tax code constraint is checked by the database, not by a tenant-filtered read
            repository.flush();
        } catch (final PersistenceException e) {
            if (violatesTaxCodeConstraint(e)) {
                throw new AssociationTaxCodeConflictException();
            }
            throw e;
        }
        return toResponse(entity);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether a persistence failure is the violation of the unique tax code constraint.
     *
     * @param failure the failure raised by the flush
     * @return true when the cause chain holds a violation of {@code association_uq}
     */
    private boolean violatesTaxCodeConstraint(final PersistenceException failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && TAX_CODE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause() == cause ? null : cause.getCause();
        }
        return false;
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
