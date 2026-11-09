package it.nexoapp.usr.api.association.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Profile of the caller's association. It carries no audit column and no internal identifier other than the
 * association id. The tax code is the public fiscal identifier of the association, not personal data.
 *
 * @param id               identifier of the association
 * @param name             name of the association
 * @param taxCode          tax code of the association
 * @param registeredOffice registered office
 * @param affiliationBody  sports body the association is affiliated with, null when none
 * @param affiliationCode  affiliation code issued by the sports body, null when none
 */
@Schema(description = "Profile of the association of the caller")
public record AssociationResponse(
        @Schema(description = "Identifier of the association", examples = "a55d0000-0000-4000-8000-00000000000a", required = true)
        UUID id,
        @Schema(description = "Name of the association", examples = "ASD Nexo", required = true)
        String name,
        @Schema(description = "Tax code of the association", examples = "97000000010", required = true)
        String taxCode,
        @Schema(description = "Registered office", required = true)
        RegisteredOffice registeredOffice,
        @Schema(description = "Sports body the association is affiliated with, null when none")
        String affiliationBody,
        @Schema(description = "Affiliation code issued by the sports body, null when none")
        String affiliationCode
) {

}
