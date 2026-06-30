package it.nexoapp.usr.resource.address.entity;

import it.nexoapp.common.hibernate.entity.AuditEntity;
import it.nexoapp.usr.resource.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
public class AddressEntity extends AuditEntity implements Serializable {

    private static final long serialVersionUID = 917822384733666966L;

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

    /**
     * Represents the association to the user entity.
     */
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "id",
            nullable = false
    )
    private UserEntity user;

    /* address data */
    /**
     * Street name with the house number, free text.
     */
    @Column(
            name = "street",
            columnDefinition = "TEXT",
            nullable = false
    )
    private String street;

    /**
     * City name, free text.
     */
    @Column(
            name = "city",
            columnDefinition = "VARCHAR(40)",
            nullable = false
    )
    private String city;

    /**
     * Zipcode, should be a valid 6 digit code.
     */
    @Column(
            name = "zip_code",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String zipCode;

    /**
     * Province, represent by istat code.
     */
    @Column(
            name = "province",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String province;

    /**
     * State, represent by istat code.
     */
    @Column(
            name = "state",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String state;

}
