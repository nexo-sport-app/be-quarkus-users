package it.nexoapp.usr.api.me.dto;

import it.nexoapp.common.security.Role;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

/**
 * The caller as the platform sees them: whether the identity is linked to a membership and, when it is, the
 * association, the member and the roles held there. Built from the caller's own principal only.
 *
 * @param linked        whether the identity is linked to a membership of the active association
 * @param associationId the active association, null when not linked
 * @param memberId      the member inside the association, null when not linked
 * @param roles         the roles held in the active association, empty when not linked
 */
@Schema(description = "The authenticated caller and their membership, if any")
public record MeResponse(
        @Schema(description = "Whether the identity is linked to a membership", examples = "true", required = true)
        boolean linked,
        @Schema(description = "Active association, null when not linked", examples = "a55d0000-0000-4000-8000-00000000000a")
        UUID associationId,
        @Schema(description = "Member inside the association, null when not linked")
        UUID memberId,
        @Schema(description = "Roles held in the active association", examples = "[\"PRESIDENT\"]", required = true)
        Set<Role> roles
) {

}
