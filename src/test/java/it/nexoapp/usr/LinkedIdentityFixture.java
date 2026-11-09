package it.nexoapp.usr;

import io.quarkus.narayana.jta.QuarkusTransaction;
import it.nexoapp.common.tenant.TenantContext;
import jakarta.persistence.EntityManager;

import java.util.function.Supplier;

/**
 * Test helper over the identity tables, which hold native-SQL-only fixtures: Hibernate never filters native
 * queries, so every call here is explicitly bound to one association and runs unscoped in its own transaction.
 */
public final class LinkedIdentityFixture {

    /** Realm subject prefix shared by every fixed Keycloak user. */
    public static final String SUBJECT_PREFIX = "00000000-0000-4000-8000-00000000";

    /** Fixed subject of the realm user {@code president}. */
    public static final String PRESIDENT_SUBJECT = SUBJECT_PREFIX + "0001";

    /** Fixed subject of the realm user {@code vice-president}. */
    public static final String VICE_PRESIDENT_SUBJECT = SUBJECT_PREFIX + "0002";

    /** Fixed subject of the realm user {@code stranger}. */
    public static final String STRANGER_SUBJECT = SUBJECT_PREFIX + "0009";

    /** Fixed subject of the realm user {@code unverified}. */
    public static final String UNVERIFIED_SUBJECT = SUBJECT_PREFIX + "0010";

    private LinkedIdentityFixture() {
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Removes every identity link, role and membership of an association.
     *
     * @param entityManager the entity manager
     * @param associationId the association whose identities are removed
     */
    public static void clean(final EntityManager entityManager, final String associationId) {
        unscoped(() -> {
            for (final String table : new String[]{"identity_link", "membership_role", "membership"}) {
                entityManager.createNativeQuery("DELETE FROM users." + table + " WHERE tenant_id = CAST(:t AS UUID)")
                        .setParameter("t", associationId).executeUpdate();
            }
            return null;
        });
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Counts the rows of a table of the users schema that belong to an association.
     *
     * @param entityManager the entity manager
     * @param table         table name in the users schema
     * @param associationId the association
     * @return the number of rows
     */
    public static int count(final EntityManager entityManager, final String table, final String associationId) {
        return unscoped(() -> ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM users." + table + " WHERE tenant_id = CAST(:t AS UUID)")
                .setParameter("t", associationId).getSingleResult()).intValue());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Counts the identity links of a subject, in any association.
     *
     * @param entityManager the entity manager
     * @param subject       token subject
     * @return the number of identity links
     */
    public static int linksOfSubject(final EntityManager entityManager, final String subject) {
        return unscoped(() -> ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM users.identity_link WHERE subject = :s")
                .setParameter("s", subject).getSingleResult()).intValue());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Counts the memberships of an association that hold a role.
     *
     * @param entityManager the entity manager
     * @param role          role name
     * @param associationId the association
     * @return the number of role rows
     */
    public static int countRole(final EntityManager entityManager, final String role, final String associationId) {
        return unscoped(() -> ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM users.membership_role WHERE role = :r "
                        + "AND tenant_id = CAST(:t AS UUID)")
                .setParameter("r", role).setParameter("t", associationId).getSingleResult()).intValue());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Runs a database task unscoped and in its own transaction.
     *
     * @param task the task
     * @param <T>  result type
     * @return the task result
     */
    private static <T> T unscoped(final Supplier<T> task) {
        return TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew().call(task::get));
    }

}
