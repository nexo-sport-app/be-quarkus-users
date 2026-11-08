package it.nexoapp.usr.api.association.entity;

import it.nexoapp.common.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * The association (ASD): the tenant root of the platform. Its identifier is assigned by the application
 * and always equals its tenant identifier (the table enforces {@code tenant_id = id}), so the row is created
 * inside its own tenant scope leaving the tenant column to Hibernate.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "association",
        uniqueConstraints = @UniqueConstraint(
                name = "association_uq",
                columnNames = {"tax_code"}
        )
)
public class AssociationEntity extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 5196304417282209741L;

    /* identifiers */
    /** Unique identifier of the association, equal to its tenant identifier. */
    @Id
    @Column(
            name = "id",
            columnDefinition = "UUID",
            nullable = false,
            unique = true
    )
    private UUID id;

    /** Name of the association. */
    @Column(
            name = "name",
            columnDefinition = "VARCHAR(120)",
            nullable = false
    )
    private String name;

    /** Tax code of the association, unique across all associations. */
    @Column(
            name = "tax_code",
            columnDefinition = "VARCHAR(16)",
            nullable = false
    )
    private String taxCode;

    /* office data */
    /** Street and number of the registered office. */
    @Column(
            name = "office_street",
            columnDefinition = "VARCHAR(60)",
            nullable = false
    )
    private String officeStreet;

    /** Postal code of the registered office. */
    @Column(
            name = "office_zip_code",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String officeZipCode;

    /** City of the registered office. */
    @Column(
            name = "office_city",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String officeCity;

    /** Province code of the registered office. */
    @Column(
            name = "office_province",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String officeProvince;

    /** Country code of the registered office. */
    @Column(
            name = "office_state",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String officeState;

    /* affiliation data */
    /** Sports body the association is affiliated with, if any. */
    @Column(
            name = "affiliation_body",
            columnDefinition = "VARCHAR(60)"
    )
    private String affiliationBody;

    /** Affiliation code issued by the sports body, if any. */
    @Column(
            name = "affiliation_code",
            columnDefinition = "VARCHAR(40)"
    )
    private String affiliationCode;

}
