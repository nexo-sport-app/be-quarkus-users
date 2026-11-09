package it.nexoapp.usr.api.contact.entity;

import it.nexoapp.common.tenant.TenantEntity;
import it.nexoapp.usr.api.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Persistent contact record of the members area.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contacts")
public class ContactEntity extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -5484895399744587235L;

    /* identifiers */
    /** Unique identifier of the contact. */
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

    /** Owner of this contact. */
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "id",
            nullable = false
    )
    private UserEntity user;

    /* contact data */

    /** Channel through which the contact can be reached. */
    @Column(
            name = "type",
            columnDefinition = "VARCHAR(15)",
            nullable = false
    )
    @Enumerated(EnumType.STRING)
    private Type type;

    /** Importance level of this contact relative to the others. */
    @Column(
            name = "priority",
            columnDefinition = "VARCHAR(15)",
            nullable = false
    )
    @Enumerated(EnumType.STRING)
    private Priority priority;

    /** Short free-text note describing the contact. */
    @Column(
            name = "description",
            columnDefinition = "VARCHAR(60)",
            nullable = false
    )
    private String description;

    /** Actual contact value, formatted according to the selected {@link Type}. */
    @Column(
            name = "value",
            columnDefinition = "VARCHAR(60)",
            nullable = false
    )
    private String value;

    /** Supported contact channels. */
    public enum Type {
        EMAIL, PEC, PHONE, FAX
    }

    /** Relative importance of a contact. */
    public enum Priority {
        EMERGENCY, PRIMARY, SECONDARY, OTHER
    }

}