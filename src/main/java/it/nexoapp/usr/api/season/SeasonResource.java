package it.nexoapp.usr.api.season;

import io.quarkus.security.Authenticated;
import it.nexoapp.usr.api.season.dto.SeasonCreateRequest;
import it.nexoapp.usr.api.season.dto.SeasonResponse;
import it.nexoapp.usr.api.season.service.SeasonService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

/**
 * The seasons of the caller's association. Reachable only by linked members: the common identity gate refuses
 * unlinked callers (D-04). Only the President creates seasons.
 */
@Authenticated
@Path("/seasons")
@RequiredArgsConstructor
public class SeasonResource {

    /* - service - */
    private final SeasonService service;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a season of the caller's association. Only the President may write.
     *
     * @param request the season, either from a start year or from an explicit range
     * @return 201 with the stored season
     */
    @POST
    @RolesAllowed("PRESIDENT")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create season", description = "Creates a season from a start year or an explicit range; President only")
    @APIResponse(responseCode = "201", description = "The created season")
    @APIResponse(responseCode = "400", description = "The request is invalid; every violation is listed")
    @APIResponse(responseCode = "403", description = "The caller is not linked, or is not the President")
    @APIResponse(responseCode = "409", description = "The range overlaps an existing season of the association")
    public Response create(@Valid @NotNull final SeasonCreateRequest request) {
        final SeasonResponse created = service.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Lists the seasons of the caller's association, the earliest first.
     *
     * @return the seasons ordered by start date ascending
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List seasons", description = "The seasons of the association of the caller, by start date")
    @APIResponse(responseCode = "200", description = "The seasons, earliest first")
    @APIResponse(responseCode = "403", description = "The caller is not linked to a membership")
    public List<SeasonResponse> list() {
        return service.list();
    }

}
