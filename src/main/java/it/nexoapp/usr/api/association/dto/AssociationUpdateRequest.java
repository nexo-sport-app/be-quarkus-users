package it.nexoapp.usr.api.association.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request to update the profile of the caller's association. Every field replaces the stored value: the
 * affiliation fields are cleared when null. Sizes match the database columns.
 *
 * @param name            name of the association
 * @param taxCode         tax code of the association: eleven digits or sixteen alphanumeric characters
 * @param office          registered office
 * @param affiliationBody sports body the association is affiliated with, null when none
 * @param affiliationCode affiliation code issued by the sports body, null when none
 */
@Schema(description = "Update of the association profile")
public record AssociationUpdateRequest(
        @Schema(description = "Name of the association", examples = "ASD Nexo", required = true)
        @NotBlank @Size(max = 120)
        String name,
        @Schema(description = "Tax code: 11 digits or 16 uppercase alphanumeric characters", examples = "97000000010", required = true)
        @NotBlank @Pattern(regexp = "^[0-9]{11}$|^[A-Z0-9]{16}$")
        String taxCode,
        @Schema(description = "Registered office", required = true)
        @NotNull @Valid
        RegisteredOfficeRequest office,
        @Schema(description = "Sports body the association is affiliated with, null when none")
        @Size(max = 60)
        String affiliationBody,
        @Schema(description = "Affiliation code issued by the sports body, null when none")
        @Size(max = 40)
        String affiliationCode
) {

}
