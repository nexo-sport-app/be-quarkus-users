package it.nexoapp.usr.api.season.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Request to create a season. Exactly one form is accepted: a start year, giving the default season from
 * 1 September of that year to 31 August of the next, or an explicit start and end date.
 *
 * @param startYear year in which the default season starts, null when explicit dates are given
 * @param startDate first day of an explicit season, inclusive, null when a start year is given
 * @param endDate   last day of an explicit season, inclusive, null when a start year is given
 */
@Schema(description = "Creation of a season, from a start year or from an explicit date range")
public record SeasonCreateRequest(
        @Schema(description = "Year the default season starts in (1 September to 31 August of the next year)", examples = "2026")
        @Min(2000) @Max(2100)
        Integer startYear,
        @Schema(description = "First day of an explicit season, inclusive", examples = "2026-09-01")
        LocalDate startDate,
        @Schema(description = "Last day of an explicit season, inclusive", examples = "2027-08-31")
        LocalDate endDate
) {

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Checks that exactly one form is given: a start year alone, or both dates and no start year.
     *
     * @return true when the request names exactly one complete form
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Give either startYear or both startDate and endDate")
    public boolean isRangeSpecified() {
        final boolean year = startYear != null;
        final boolean dates = startDate != null && endDate != null;
        final boolean someDate = startDate != null || endDate != null;
        return year ? !someDate : dates;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Checks that an explicit range ends after it starts.
     *
     * @return true when no explicit range is given or its end is after its start
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "endDate must be after startDate")
    public boolean isEndAfterStart() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }

}
