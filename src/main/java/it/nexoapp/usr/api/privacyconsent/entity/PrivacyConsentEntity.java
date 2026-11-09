package it.nexoapp.usr.api.privacyconsent.entity;

import it.nexoapp.common.tenant.TenantEntity;
import it.nexoapp.usr.api.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Persistent privacy consent record of the members area.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "privacy_consent",
        uniqueConstraints = @UniqueConstraint(
                name = "privacy_consent_uq",
                columnNames = { "tenant_id", "user_id", "code" }
        )
)
public class PrivacyConsentEntity extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 2035203239465595406L;

    /* identifiers */
    /** Unique identifier of the privacy consent. */
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

    /** Owner of this privacy consent. */
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "id",
            nullable = false
    )
    private UserEntity user;

    /** Code identifying the type of privacy consent. */
    @Column(
            name = "code",
            columnDefinition = "VARCHAR(15)",
            nullable = false
    )
    private String code;

    /* privacy consent data */

    /** Whether the user has given consent. */
    @Column(
            name = "consent",
            columnDefinition = "BOOLEAN",
            nullable = false
    )
    private Boolean consent;

}