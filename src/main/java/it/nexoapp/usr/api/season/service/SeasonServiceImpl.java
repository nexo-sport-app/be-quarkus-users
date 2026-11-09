package it.nexoapp.usr.api.season.service;

import it.nexoapp.usr.api.season.dto.SeasonCreateRequest;
import it.nexoapp.usr.api.season.dto.SeasonResponse;
import it.nexoapp.usr.api.season.entity.SeasonEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

/**
 * Default {@link SeasonService}. Seasons are read and written through the tenant-filtered repository, so the
 * overlap rule only ever compares seasons of the same association. Only calendar dates are used: no clock.
 */
@ApplicationScoped
@RequiredArgsConstructor
class SeasonServiceImpl implements SeasonService {

    /* - constants - */
    /** Name of the unique constraint on the start date of a season. */
    private static final String START_DATE_CONSTRAINT = "season_uq";

    /* - repository - */
    private final SeasonRepository repository;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    @Transactional
    public SeasonResponse create(final SeasonCreateRequest request) {
        final LocalDate start;
        final LocalDate end;
        if (request.startYear() != null) {
            start = LocalDate.of(request.startYear(), Month.SEPTEMBER, 1);
            end = LocalDate.of(request.startYear() + 1, Month.AUGUST, 31);
        } else if (request.startDate() != null && request.endDate() != null) {
            start = request.startDate();
            end = request.endDate();
        } else {
            throw new IllegalArgumentException("A season needs a start year or both a start and an end date");
        }

        if (repository.existsOverlapping(start, end)) {
            throw new SeasonOverlapException();
        }

        final SeasonEntity entity = SeasonEntity.builder()
                .label(start.getYear() + "/" + end.getYear())
                .startDate(start)
                .endDate(end)
                .build();
        repository.persist(entity);
        try {
            // flush now: a concurrent season with the same start date is caught by the unique constraint
            repository.flush();
        } catch (final PersistenceException e) {
            if (violatesStartDateConstraint(e)) {
                throw new SeasonOverlapException();
            }
            throw e;
        }
        return toResponse(entity);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public List<SeasonResponse> list() {
        return repository.findAllByStartDate().stream().map(this::toResponse).toList();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether a persistence failure is the violation of the unique start date constraint.
     *
     * @param failure the failure raised by the flush
     * @return true when the cause chain holds a violation of {@code season_uq}
     */
    private boolean violatesStartDateConstraint(final PersistenceException failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && START_DATE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause() == cause ? null : cause.getCause();
        }
        return false;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Maps the entity to the public season, leaving out audit columns and the tenant column.
     *
     * @param entity the season
     * @return the response
     */
    private SeasonResponse toResponse(final SeasonEntity entity) {
        return new SeasonResponse(entity.getId(), entity.getLabel(), entity.getStartDate(), entity.getEndDate());
    }

}
