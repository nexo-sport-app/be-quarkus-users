package it.nexoapp.usr.api.user.entity;

import it.nexoapp.common.hibernate.entity.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Persistent user record of the members area.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "users_uq",
                columnNames = {"tax_code"}
        )
)
public class UserEntity extends AuditEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 8177397516775487505L;

    /* identifiers */
    /** Unique identifier of the user. */
    @Id
    @Column(
            name = "id",
            columnDefinition = "UUID",
            nullable = false,
            unique = true
    )
    private UUID id;

    /** User's first name. */
    @Column(
            name = "first_name",
            columnDefinition = "VARCHAR(40)",
            nullable = false
    )
    private String firstName;

    /** User's middle name, if any. */
    @Column(
            name = "middle_name",
            columnDefinition = "VARCHAR(40)"
    )
    private String middleName;

    /** User's last name. */
    @Column(
            name = "last_name",
            columnDefinition = "VARCHAR(40)",
            nullable = false
    )
    private String lastName;

    /** User's gender. */
    @Column(
            name = "gender",
            columnDefinition = "VARCHAR(2)"
    )
    @Enumerated(EnumType.STRING)
    private Gender gender;

    /** User's fiscal code, unique across all users. */
    @Column(
            name = "tax_code",
            columnDefinition = "VARCHAR(20)",
            nullable = false
    )
    private String taxCode;

    /** User's nationality. */
    @Column(
            name = "nationality",
            columnDefinition = "VARCHAR(20)",
            nullable = false
    )
    private String nationality;

    /** User's date of birth. */
    @Column(
            name = "date_of_birth",
            columnDefinition = "DATE"
    )
    private LocalDate dateOfBirth;

    /** User's place of birth. */
    @Column(
            name = "place_of_birth",
            columnDefinition = "VARCHAR(10)"
    )
    private String placeOfBirth;

    /** Supported gender values. */
    public enum Gender {
        M, F, O
    }

}