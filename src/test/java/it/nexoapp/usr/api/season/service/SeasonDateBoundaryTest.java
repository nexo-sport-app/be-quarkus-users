package it.nexoapp.usr.api.season.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import it.nexoapp.common.context.ExecutionScope;
import it.nexoapp.common.tenant.TenantContext;
import it.nexoapp.usr.api.season.dto.SeasonCreateRequest;
import it.nexoapp.usr.api.season.dto.SeasonResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves the season rules at service level on a real database: the September to August default, adjacency,
 * overlap, ordering, and that dates are stable on the daylight saving change days of 2026 under the Europe/Rome
 * JVM zone. Every test works in a fresh random tenant, so results never depend on other tests or on test order.
 */
@QuarkusTest
class SeasonDateBoundaryTest {

    /* - service - */
    @Inject
    SeasonService service;

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A start year alone gives the default season: 1 September to 31 August of the next year.
     */
    @Test
    void startYearGivesTheSeptemberToAugustDefault() {
        final SeasonResponse season = inFreshTenant(tenant -> service.create(year(2026)));

        assertNotNull(season.id());
        assertEquals("2026/2027", season.label());
        assertEquals(LocalDate.of(2026, 9, 1), season.startDate());
        assertEquals(LocalDate.of(2027, 8, 31), season.endDate());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * An explicit range is stored as given, with the label taken from its years.
     */
    @Test
    void explicitRangeIsStoredAsGiven() {
        final SeasonResponse season = inFreshTenant(tenant -> service.create(range("2026-01-10", "2026-06-20")));

        assertEquals("2026/2026", season.label());
        assertEquals(LocalDate.of(2026, 1, 10), season.startDate());
        assertEquals(LocalDate.of(2026, 6, 20), season.endDate());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A season starting the day after another ends is accepted.
     */
    @Test
    void adjacentSeasonIsAccepted() {
        final List<SeasonResponse> seasons = inFreshTenant(tenant -> {
            service.create(year(2026));
            service.create(range("2027-09-01", "2028-08-31"));
            return service.list();
        });

        assertEquals(2, seasons.size());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The same season twice, and a range straddling an existing season, are both refused.
     */
    @Test
    void overlappingSeasonIsRefused() {
        inFreshTenant(tenant -> {
            service.create(year(2026));

            assertThrows(SeasonOverlapException.class, () -> service.create(year(2026)));
            assertThrows(SeasonOverlapException.class, () -> service.create(range("2027-01-01", "2027-12-31")));
            assertThrows(SeasonOverlapException.class, () -> service.create(range("2025-01-01", "2026-09-01")));
            assertThrows(SeasonOverlapException.class, () -> service.create(range("2026-10-01", "2026-10-31")));
            return null;
        });
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Seasons of another association never count as overlapping: the rule is per association.
     */
    @Test
    void overlapIsPerAssociation() {
        inFreshTenant(tenant -> service.create(year(2026)));

        final SeasonResponse other = inFreshTenant(tenant -> service.create(year(2026)));

        assertEquals("2026/2027", other.label());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Listing returns the seasons of the association by start date ascending, whatever the creation order.
     */
    @Test
    void listIsOrderedByStartDateAscending() {
        final List<SeasonResponse> seasons = inFreshTenant(tenant -> {
            service.create(year(2028));
            service.create(year(2026));
            service.create(year(2027));
            return service.list();
        });

        assertEquals(List.of("2026/2027", "2027/2028", "2028/2029"), seasons.stream().map(SeasonResponse::label).toList());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The tests run with the Europe/Rome JVM zone: it guards the surefire argument the DST cases depend on.
     */
    @Test
    void jvmZoneIsEuropeRome() {
        assertEquals(ZoneId.of("Europe/Rome"), ZoneId.systemDefault());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Seasons meeting on the spring-forward day are adjacent, and the stored dates are exactly the given ones.
     */
    @Test
    void springForwardDayIsAnOrdinaryBoundary() {
        final UUID tenant = UUID.randomUUID();
        final List<SeasonResponse> seasons = scoped(tenant, () -> {
            service.create(range("2026-03-01", "2026-03-28"));
            service.create(range("2026-03-29", "2026-10-25"));
            return service.list();
        });

        assertEquals(LocalDate.of(2026, 3, 29), seasons.get(1).startDate());
        assertEquals(LocalDate.of(2026, 10, 25), seasons.get(1).endDate());

        final Object[] raw = inTenant(tenant, () -> (Object[]) entityManager
                .createNativeQuery("SELECT CAST(start_date AS TEXT), CAST(end_date AS TEXT) FROM users.season "
                        + "WHERE tenant_id = :tenant AND start_date = DATE '2026-03-29'")
                .setParameter("tenant", tenant)
                .getSingleResult());
        assertEquals("2026-03-29", raw[0]);
        assertEquals("2026-10-25", raw[1]);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A season sharing the fall-back day is refused; one starting the day after is accepted.
     */
    @Test
    void fallBackDayIsAnOrdinaryBoundary() {
        inFreshTenant(tenant -> {
            service.create(range("2026-03-29", "2026-10-25"));

            assertThrows(SeasonOverlapException.class, () -> service.create(range("2026-10-25", "2026-12-31")));
            final SeasonResponse next = service.create(range("2026-10-26", "2026-12-31"));

            assertEquals(LocalDate.of(2026, 10, 26), next.startDate());
            assertEquals(LocalDate.of(2026, 12, 31), next.endDate());
            return null;
        });
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Builds a default-season request.
     *
     * @param startYear year the season starts in
     * @return the request
     */
    private static SeasonCreateRequest year(final int startYear) {
        return new SeasonCreateRequest(startYear, null, null);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Builds an explicit-range request.
     *
     * @param start first day, ISO format
     * @param end   last day, ISO format
     * @return the request
     */
    private static SeasonCreateRequest range(final String start, final String end) {
        return new SeasonCreateRequest(null, LocalDate.parse(start), LocalDate.parse(end));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Runs a call in a fresh random tenant, as an identity provider actor. No transaction is opened around the
     * call: every service call commits (or rolls back) on its own, as it does for a request, so a refused
     * creation never poisons the calls that follow it.
     *
     * @param call call to run, receiving the tenant
     * @param <T>  type of the result
     * @return the value returned by the call
     */
    private static <T> T inFreshTenant(final java.util.function.Function<UUID, T> call) {
        final UUID tenant = UUID.randomUUID();
        return scoped(tenant, () -> call.apply(tenant));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Runs a call inside the tenant scope as an identity provider actor, with no transaction around it.
     *
     * @param tenant tenant of the scope
     * @param call   call to run
     * @param <T>    type of the result
     * @return the value returned by the call
     */
    private static <T> T scoped(final UUID tenant, final Supplier<T> call) {
        return ExecutionScope.callAs("idp:season-test", () -> TenantContext.callAs(tenant, call));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Runs a call inside the tenant scope and, inside it, a new transaction (scope before transaction), as an
     * identity provider actor.
     *
     * @param tenant tenant of the scope
     * @param call   call to run
     * @param <T>    type of the result
     * @return the value returned by the call
     */
    private static <T> T inTenant(final UUID tenant, final Supplier<T> call) {
        return ExecutionScope.callAs("idp:season-test",
                () -> TenantContext.callAs(tenant, () -> QuarkusTransaction.requiringNew().call(call::get)));
    }

}
