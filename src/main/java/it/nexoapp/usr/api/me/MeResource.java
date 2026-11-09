package it.nexoapp.usr.api.me;

import io.quarkus.security.Authenticated;
import it.nexoapp.common.security.AllowUnlinked;
import it.nexoapp.usr.api.me.dto.MeResponse;
import it.nexoapp.usr.api.me.service.MeService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * The caller's own identity. Reachable by any authenticated identity, linked or not (D-04), so that the
 * frontend can route a new user to onboarding.
 */
@AllowUnlinked
@Authenticated
@Path("/me")
@RequiredArgsConstructor
public class MeResource {

    /* - service - */
    private final MeService service;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns who the authenticated caller is and which membership they hold.
     *
     * @return the caller description
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Who am I", description = "The caller and their membership, if linked")
    @APIResponse(responseCode = "200", description = "The caller")
    public MeResponse whoAmI() {
        return service.whoAmI();
    }

}
