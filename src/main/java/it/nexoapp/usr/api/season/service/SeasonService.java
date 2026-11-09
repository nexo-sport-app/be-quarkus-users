package it.nexoapp.usr.api.season.service;

import it.nexoapp.usr.api.season.dto.SeasonCreateRequest;
import it.nexoapp.usr.api.season.dto.SeasonResponse;

import java.util.List;

/**
 * Creates and lists the seasons of the caller's association. The association is the resolved tenant of the
 * request, never client input.
 */
public interface SeasonService {

    /**
     * Creates a season, either from a start year (1 September of that year to 31 August of the next) or from an
     * explicit range. Dates come only from the request, never from the current time.
     *
     * @param request the season to create
     * @return the stored season
     * @throws SeasonOverlapException when the range shares a day with an existing season of the association
     */
    SeasonResponse create(SeasonCreateRequest request);

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Lists the seasons of the association.
     *
     * @return the seasons ordered by start date ascending
     */
    List<SeasonResponse> list();

}
