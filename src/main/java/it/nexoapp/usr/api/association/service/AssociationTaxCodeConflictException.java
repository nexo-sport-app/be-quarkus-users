package it.nexoapp.usr.api.association.service;

import it.nexoapp.common.exception.AbstractRuntimeException;
import jakarta.ws.rs.core.Response;

import java.io.Serial;

/**
 * Raised when an association update would give it a tax code that is already registered to another association.
 * The tax code is unique across all associations, but associations cannot see each other, so the collision is
 * detected from the database constraint. The message is generic on purpose: it names no association.
 */
public class AssociationTaxCodeConflictException extends AbstractRuntimeException {

    /** Fault code of the conflict. */
    public static final String CODE = "ASSOCIATION_TAX_CODE_CONFLICT";

    /** Generic message, revealing nothing about the other association. */
    public static final String MESSAGE = "Tax code already registered to another association";

    @Serial
    private static final long serialVersionUID = 8240915663310927145L;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates the exception with the fixed code and generic message, mapped to 409.
     */
    public AssociationTaxCodeConflictException() {
        super(Response.Status.CONFLICT, CODE, MESSAGE);
    }

}
