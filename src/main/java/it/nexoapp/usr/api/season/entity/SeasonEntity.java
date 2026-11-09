package it.nexoapp.usr.api.season.entity;

import it.nexoapp.common.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A sports season of one association, a closed range of calendar days. Tenant-owned: seasons of one association
 * are never visible under another. The dates are plain calendar dates with no time zone; seasons of the same
 * association never overlap and never start on the same day.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "season",
        uniqueConstraints = @UniqueConstraint(
                name = "season_uq",
                columnNames = {"tenant_id", "start_date"}
        )
)
public class SeasonEntity extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 4417209385516640283L;

    /* identifiers */
    /** Unique identifier of the season. */
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(
            name = "id",
            columnDefinition = "UUID",
            nullable = false,
            unique = true
    )
    private UUID id;

    /* season data */
    /** Label of the season in the form {@code YYYY/YYYY}, from the years of its first and last day. */
    @Column(
            name = "label",
            columnDefinition = "VARCHAR(9)",
            nullable = false
    )
    private String label;

    /** First day of the season, inclusive. */
    @Column(
            name = "start_date",
            columnDefinition = "DATE",
            nullable = false
    )
    private LocalDate startDate;

    /** Last day of the season, inclusive. */
    @Column(
            name = "end_date",
            columnDefinition = "DATE",
            nullable = false
    )
    private LocalDate endDate;

}
