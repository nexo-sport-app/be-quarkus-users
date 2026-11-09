package it.nexoapp.usr.api.association;

import io.quarkus.security.Authenticated;
import it.nexoapp.usr.api.association.dto.AssociationResponse;
import it.nexoapp.usr.api.association.dto.AssociationUpdateRequest;
import it.nexoapp.usr.api.association.service.AssociationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * The association of the caller. Reachable only by linked members: the common identity gate refuses
 * unlinked callers (D-04), so this resource is never exempted from it.
 */
@Authenticated
@Path("/association")
@RequiredArgsConstructor
public class AssociationResource {

    /* - service - */
    private final AssociationService service;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns the profile of the association the caller is a member of.
     *
     * @return the association profile
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Association profile", description = "The association of the caller")
    @APIResponse(responseCode = "200", description = "The association profile")
    @APIResponse(responseCode = "403", description = "The caller is not linked to a membership")
    public AssociationResponse getCurrent() {
        return service.getCurrent();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Replaces the profile of the association the caller is a member of. Only the President may write.
     *
     * @param request the new profile
     * @return the updated association profile
     */
    @PUT
    @RolesAllowed("PRESIDENT")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update association profile", description = "Replaces the profile of the association of the caller; President only")
    @APIResponse(responseCode = "200", description = "The updated association profile")
    @APIResponse(responseCode = "400", description = "The request is invalid; every violation is listed")
    @APIResponse(responseCode = "403", description = "The caller is not linked, or is not the President")
    @APIResponse(responseCode = "409", description = "The tax code is already registered to another association")
    public AssociationResponse update(@Valid @NotNull final AssociationUpdateRequest request) {
        return service.update(request);
    }

}
