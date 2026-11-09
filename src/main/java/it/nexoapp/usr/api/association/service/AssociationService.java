package it.nexoapp.usr.api.association.service;

import it.nexoapp.usr.api.association.dto.AssociationResponse;

/**
 * Reads the association profile of the caller.
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

}
