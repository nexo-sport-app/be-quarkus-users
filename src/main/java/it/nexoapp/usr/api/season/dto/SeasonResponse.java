package it.nexoapp.usr.api.season.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A season of the caller's association. It carries no audit column and no tenant identifier.
 *
 * @param id        identifier of the season
 * @param label     label of the season in the form {@code YYYY/YYYY}
 * @param startDate first day of the season, inclusive
 * @param endDate   last day of the season, inclusive
 */
@Schema(description = "A season of the association")
public record SeasonResponse(
        @Schema(description = "Identifier of the season", examples = "0190a5c0-0000-7000-8000-000000000001", required = true)
        UUID id,
        @Schema(description = "Label of the season", examples = "2026/2027", required = true)
        String label,
        @Schema(description = "First day of the season, inclusive", examples = "2026-09-01", required = true)
        LocalDate startDate,
        @Schema(description = "Last day of the season, inclusive", examples = "2027-08-31", required = true)
        LocalDate endDate
) {

}
