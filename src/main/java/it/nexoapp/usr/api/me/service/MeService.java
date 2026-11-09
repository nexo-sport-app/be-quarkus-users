package it.nexoapp.usr.api.me.service;

import it.nexoapp.usr.api.me.dto.MeResponse;

/**
 * Answers "who am I" for the authenticated caller.
 */
public interface MeService {

    /**
     * Describes the caller from the principal resolved for the current request.
     *
     * @return the caller, with {@code linked=false}, null identifiers and no roles when not linked
     */
    MeResponse whoAmI();

}
