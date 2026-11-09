package it.nexoapp.usr.api.association.service;

import it.nexoapp.usr.api.association.dto.AssociationResponse;
import it.nexoapp.usr.api.association.dto.AssociationUpdateRequest;

/**
 * Reads and updates the association profile of the caller.
 */
public interface AssociationService {

    /**
     * Reads the association the current request acts in. The association is selected by the resolved tenant of
     * the request, never by client input.
     *
     * @return the association profile
     * @throws it.nexoapp.common.hibernate.exception.EntityNotFoundException when the tenant has no association row
     */
    AssociationResponse getCurrent();

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Replaces the profile of the association the current request acts in. The association is selected by the
     * resolved tenant of the request, never by client input. The write is flushed before returning, so a tax code
     * already registered to another association is refused here.
     *
     * @param request the new profile
     * @return the updated association profile
     * @throws it.nexoapp.common.hibernate.exception.EntityNotFoundException when the tenant has no association row
     * @throws AssociationTaxCodeConflictException                            when the tax code belongs to another association
     */
    AssociationResponse update(AssociationUpdateRequest request);

}
