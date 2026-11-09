package it.nexoapp.usr.api.association.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Registered office of the association.
 *
 * @param street   street and number
 * @param zipCode  postal code
 * @param city     city
 * @param province province code
 * @param state    country code
 */
@Schema(description = "Registered office of the association")
public record RegisteredOffice(
        @Schema(description = "Street and number", examples = "Via Roma 1", required = true)
        String street,
        @Schema(description = "Postal code", examples = "00100", required = true)
        String zipCode,
        @Schema(description = "City", examples = "Roma", required = true)
        String city,
        @Schema(description = "Province code", examples = "RM", required = true)
        String province,
        @Schema(description = "Country code", examples = "IT", required = true)
        String state
) {

}
