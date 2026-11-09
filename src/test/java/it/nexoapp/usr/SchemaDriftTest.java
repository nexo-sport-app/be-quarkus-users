package it.nexoapp.usr;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import it.nexoapp.common.tenant.TenantContext;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares every entity of the users persistence unit (including the messaging outbox and inbox) with the SQL
 * schema built by Flyway. Hibernate validation only checks column types: this test also checks the declared
 * length, precision, nullability, the unique constraint names with their columns, and that no table or column
 * exists on one side only.
 */
@QuarkusTest
class SchemaDriftTest {

    /** Postgres schema owned by the users service. */
    private static final String SCHEMA = "users";

    /** Table written by Flyway itself, not mapped by any entity. */
    private static final String FLYWAY_TABLE = "flyway_schema_history";

    /** Parses a column definition such as {@code VARCHAR(20)} or {@code NUMERIC(10,2)}. */
    private static final Pattern DEFINITION = Pattern.compile("^([A-Z ]+?)\\s*(?:\\((\\d+)(?:\\s*,\\s*(\\d+))?\\))?$");

    /** Column definition base type to the name used by {@code information_schema.columns.data_type}. */
    private static final Map<String, String> DATA_TYPES = Map.ofEntries(
            Map.entry("UUID", "uuid"),
            Map.entry("TEXT", "text"),
            Map.entry("VARCHAR", "character varying"),
            Map.entry("CHAR", "character"),
            Map.entry("TIMESTAMPTZ", "timestamp with time zone"),
            Map.entry("TIMESTAMP", "timestamp without time zone"),
            Map.entry("INTEGER", "integer"),
            Map.entry("INT", "integer"),
            Map.entry("SMALLINT", "smallint"),
            Map.entry("BIGINT", "bigint"),
            Map.entry("BOOLEAN", "boolean"),
            Map.entry("DATE", "date"),
            Map.entry("NUMERIC", "numeric"));

    /* - persistence - */
    @Inject
    EntityManager entityManager;

    @Inject
    EntityManagerFactory entityManagerFactory;

    /**
     * A column as declared by an entity mapping.
     *
     * @param table     table name
     * @param column    column name
     * @param dataType  expected {@code information_schema} data type, or null when the mapping does not declare one
     * @param length    expected character maximum length, or null when not bounded
     * @param precision expected numeric precision, or null when not declared
     * @param scale     expected numeric scale, or null when not declared
     * @param nullable  whether the mapping allows null
     */
    private record Mapped(String table, String column, String dataType, Integer length, Integer precision,
                          Integer scale, boolean nullable) {
    }

    /**
     * A column as found in the database.
     *
     * @param dataType  {@code information_schema} data type
     * @param length    character maximum length, or null
     * @param precision numeric precision, or null
     * @param scale     numeric scale, or null
     * @param nullable  whether the column accepts null
     */
    private record Actual(String dataType, Integer length, Integer precision, Integer scale, boolean nullable) {
    }

    /**
     * A unique constraint, as declared or as found.
     *
     * @param table   table name
     * @param name    constraint name
     * @param columns ordered column names joined by a comma
     */
    private record Unique(String table, String name, String columns) {
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Every mapped column exists with the declared type, length, precision and nullability, and every database
     * column of a mapped table is mapped.
     */
    @Test
    void columnsMatchTheMappings() {
        final Map<String, Mapped> mapped = new HashMap<>();
        for (final Class<?> type : entityClasses()) {
            for (final Mapped column : mappedColumns(type)) {
                mapped.put(column.table() + "." + column.column(), column);
            }
        }
        final Map<String, Actual> actual = actualColumns();
        final Set<String> tables = new HashSet<>();
        mapped.values().forEach(column -> tables.add(column.table()));

        final List<String> drift = new ArrayList<>();
        mapped.forEach((key, column) -> {
            final Actual found = actual.get(key);
            if (found == null) {
                drift.add(key + ": mapped but missing in the database");
                return;
            }
            if (column.dataType() != null && !column.dataType().equals(found.dataType())) {
                drift.add(key + ": data type " + found.dataType() + " in the database, " + column.dataType() + " mapped");
            }
            if (column.dataType() != null && !Objects.equals(column.length(), found.length())) {
                drift.add(key + ": length " + found.length() + " in the database, " + column.length() + " mapped");
            }
            if (column.precision() != null && !column.precision().equals(found.precision())) {
                drift.add(key + ": precision " + found.precision() + " in the database, " + column.precision() + " mapped");
            }
            if (column.scale() != null && !column.scale().equals(found.scale())) {
                drift.add(key + ": scale " + found.scale() + " in the database, " + column.scale() + " mapped");
            }
            if (column.nullable() != found.nullable()) {
                drift.add(key + ": nullable " + found.nullable() + " in the database, " + column.nullable() + " mapped");
            }
        });
        actual.keySet().stream()
                .filter(key -> tables.contains(key.substring(0, key.indexOf('.'))) && !mapped.containsKey(key))
                .forEach(key -> drift.add(key + ": in the database but not mapped"));

        assertTrue(drift.isEmpty(), "Schema drift:\n" + String.join("\n", new TreeSet<>(drift)));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Every table of the schema has an entity, and the messaging tables are part of the persistence unit.
     */
    @Test
    void everyTableHasAnEntity() {
        final Set<String> mapped = new TreeSet<>();
        entityClasses().forEach(type -> mapped.add(type.getAnnotation(Table.class).name()));
        final Set<String> tables = new TreeSet<>();
        actualColumns().keySet().forEach(key -> tables.add(key.substring(0, key.indexOf('.'))));
        tables.remove(FLYWAY_TABLE);

        assertEquals(tables, mapped);
        assertTrue(mapped.containsAll(Set.of("outbox", "inbox")), "outbox and inbox must be mapped");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The unique constraints declared on the entities exist in the database with the same name and columns, and
     * the database holds no unique constraint the entities do not declare.
     */
    @Test
    void uniqueConstraintsMatchTheMappings() {
        final Set<Unique> declared = new TreeSet<>(Comparator.comparing(Unique::name));
        for (final Class<?> type : entityClasses()) {
            final Table table = type.getAnnotation(Table.class);
            for (final UniqueConstraint constraint : table.uniqueConstraints()) {
                declared.add(new Unique(table.name(), constraint.name(), String.join(",", constraint.columnNames())));
            }
        }
        final Set<Unique> found = new TreeSet<>(Comparator.comparing(Unique::name));
        found.addAll(actualUniqueConstraints());

        assertEquals(found, declared);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Entity classes of the persistence unit.
     *
     * @return the Java types of all entities, ordered by name
     */
    private List<Class<?>> entityClasses() {
        return entityManagerFactory.getMetamodel().getEntities().stream()
                .<Class<?>>map(EntityType::getJavaType)
                .sorted(Comparator.comparing(Class::getName))
                .toList();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Columns an entity maps through {@code @Column} and {@code @JoinColumn}, own and inherited.
     *
     * @param type the entity class
     * @return the mapped columns
     */
    private List<Mapped> mappedColumns(final Class<?> type) {
        final String table = type.getAnnotation(Table.class).name();
        final List<Mapped> columns = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (final Field field : current.getDeclaredFields()) {
                final Column column = field.getAnnotation(Column.class);
                final JoinColumn join = field.getAnnotation(JoinColumn.class);
                if (column != null) {
                    columns.add(fromDefinition(table, column.name(), column.columnDefinition(), column.nullable()));
                } else if (join != null) {
                    columns.add(fromDefinition(table, join.name(), join.columnDefinition(), join.nullable()));
                }
            }
        }
        return columns;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Builds the expectation of a column from its declared definition.
     *
     * @param table      table name
     * @param name       column name
     * @param definition declared column definition, possibly empty
     * @param nullable   declared nullability
     * @return the expectation
     */
    private Mapped fromDefinition(final String table, final String name, final String definition,
                                  final boolean nullable) {
        if (definition == null || definition.isBlank()) {
            return new Mapped(table, name, null, null, null, null, nullable);
        }
        final Matcher matcher = DEFINITION.matcher(definition.trim().toUpperCase());
        assertTrue(matcher.matches(), table + "." + name + ": unreadable column definition " + definition);
        final String base = matcher.group(1);
        final String dataType = DATA_TYPES.get(base);
        assertTrue(dataType != null, table + "." + name + ": no data type mapping for " + base + ", extend this test");
        final Integer first = matcher.group(2) == null ? null : Integer.valueOf(matcher.group(2));
        final Integer second = matcher.group(3) == null ? null : Integer.valueOf(matcher.group(3));
        return switch (base) {
            case "VARCHAR", "CHAR" -> new Mapped(table, name, dataType, first, null, null, nullable);
            case "NUMERIC" -> new Mapped(table, name, dataType, null, first, second == null ? Integer.valueOf(0) : second,
                    nullable);
            default -> new Mapped(table, name, dataType, null, null, null, nullable);
        };
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Reads the columns of the users schema from the database catalog.
     *
     * @return the columns keyed by {@code table.column}
     */
    private Map<String, Actual> actualColumns() {
        final String sql = "SELECT table_name, column_name, data_type, character_maximum_length, numeric_precision, "
                + "numeric_scale, is_nullable FROM information_schema.columns WHERE table_schema = :schema";
        final Map<String, Actual> result = new HashMap<>();
        for (final Object[] row : query(sql)) {
            result.put(row[0] + "." + row[1], new Actual((String) row[2], integer(row[3]), integer(row[4]),
                    integer(row[5]), "YES".equals(row[6])));
        }
        return result;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Reads the unique constraints of the users schema from the database catalog.
     *
     * @return the unique constraints
     */
    private List<Unique> actualUniqueConstraints() {
        final String sql = "SELECT t.relname, c.conname, string_agg(a.attname, ',' ORDER BY k.ord) "
                + "FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid "
                + "JOIN pg_namespace n ON n.oid = t.relnamespace "
                + "JOIN LATERAL unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord) ON TRUE "
                + "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum "
                + "WHERE n.nspname = :schema AND c.contype = 'u' GROUP BY t.relname, c.conname";
        return query(sql).stream()
                .map(row -> new Unique((String) row[0], (String) row[1], (String) row[2]))
                .toList();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Runs a catalog query for the users schema, unscoped and in its own transaction (native SQL is never
     * tenant-filtered).
     *
     * @param sql the query, with the named parameter {@code :schema}
     * @return the rows
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> query(final String sql) {
        return TenantContext.callUnscoped(() -> QuarkusTransaction.requiringNew().call(
                () -> (List<Object[]>) entityManager.createNativeQuery(sql).setParameter("schema", SCHEMA)
                        .getResultList()));
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Converts a nullable numeric catalog value.
     *
     * @param value the value
     * @return the integer value, or null
     */
    private static Integer integer(final Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

}
