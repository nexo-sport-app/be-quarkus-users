package it.nexoapp.usr.api.season.service;

import it.nexoapp.common.exception.AbstractRuntimeException;
import jakarta.ws.rs.core.Response;

import java.io.Serial;

/**
 * Raised when a new season shares at least one day with a season the association already has.
 */
public class SeasonOverlapException extends AbstractRuntimeException {

    /** Fault code of the conflict. */
    public static final String CODE = "SEASON_OVERLAP";

    /** Message of the conflict. */
    public static final String MESSAGE = "Season overlaps an existing season of the association";

    @Serial
    private static final long serialVersionUID = 3160842995471826604L;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates the exception with the fixed code and message, mapped to 409.
     */
    public SeasonOverlapException() {
        super(Response.Status.CONFLICT, CODE, MESSAGE);
    }

}
