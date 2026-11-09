package it.nexoapp.usr.api.association.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Registered office of the association in an update request. Sizes match the database columns.
 *
 * @param street   street and number
 * @param zipCode  postal code, five digits
 * @param city     city code
 * @param province province code
 * @param state    country code
 */
@Schema(description = "Registered office of the association")
public record RegisteredOfficeRequest(
        @Schema(description = "Street and number", examples = "Via Roma 1", required = true)
        @NotBlank @Size(max = 60)
        String street,
        @Schema(description = "Postal code, five digits", examples = "00100", required = true)
        @NotBlank @Pattern(regexp = "^[0-9]{5}$")
        String zipCode,
        @Schema(description = "City code", examples = "Roma", required = true)
        @NotBlank @Size(max = 10)
        String city,
        @Schema(description = "Province code", examples = "RM", required = true)
        @NotBlank @Size(max = 10)
        String province,
        @Schema(description = "Country code", examples = "IT", required = true)
        @NotBlank @Size(max = 10)
        String state
) {

}
