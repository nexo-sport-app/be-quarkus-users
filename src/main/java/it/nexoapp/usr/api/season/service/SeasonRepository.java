package it.nexoapp.usr.api.season.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import it.nexoapp.usr.api.season.entity.SeasonEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Persistence of {@link SeasonEntity}. Package-private: only the season services use it.
 * Every query runs inside the tenant of the active scope, so it only ever sees the seasons of that association.
 */
@ApplicationScoped
class SeasonRepository implements PanacheRepositoryBase<SeasonEntity, UUID> {

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether a season of the current association shares at least one day with the given range.
     *
     * @param start first day of the range, inclusive
     * @param end   last day of the range, inclusive
     * @return true when an existing season has {@code startDate <= end} and {@code endDate >= start}
     */
    boolean existsOverlapping(final LocalDate start, final LocalDate end) {
        return count("startDate <= ?1 and endDate >= ?2", end, start) > 0;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Lists the seasons of the current association, the earliest first.
     *
     * @return the seasons ordered by start date ascending
     */
    List<SeasonEntity> findAllByStartDate() {
        return listAll(Sort.ascending("startDate"));
    }

}
