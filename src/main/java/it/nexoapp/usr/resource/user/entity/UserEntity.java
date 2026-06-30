package it.nexoapp.usr.resource.user.entity;

import it.nexoapp.common.hibernate.entity.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

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
    /**
     * Identifies the user entity uniquely.
     */
    @Id
    @Column(
            name = "id",
            columnDefinition = "UUID",
            nullable = false,
            unique = true
    )
    private UUID id;

    @Column(
            name = "first_name",
            columnDefinition = "VARCHAR(40)",
            nullable = false
    )
    private String firstName;

    @Column(
            name = "middle_name",
            columnDefinition = "VARCHAR(40)"
    )
    private String middleName;

    @Column(
            name = "last_name",
            columnDefinition = "VARCHAR(40)",
            nullable = false
    )
    private String lastName;

    @Column(
            name = "gender",
            columnDefinition = "VARCHAR(2)"
    )
    private String gender;

    @Column(
            name = "tax_code",
            columnDefinition = "VARCHAR(20)",
            nullable = false
    )
    private String taxCode;

    @Column(
            name = "nationality",
            columnDefinition = "VARCHAR(20)",
            nullable = false
    )
    private String nationality;

    @Column(
            name = "date_of_birth",
            columnDefinition = "DATE"
    )
    private LocalDate dateOfBirth;

    @Column(
            name = "place_of_birth",
            columnDefinition = "VARCHAR(10)"
    )
    private String placeOfBirth;

}