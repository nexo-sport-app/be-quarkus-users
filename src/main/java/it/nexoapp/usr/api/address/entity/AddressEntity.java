package it.nexoapp.usr.api.address.entity;

import it.nexoapp.common.tenant.TenantEntity;
import it.nexoapp.usr.api.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Persistent address record of the members area.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
public class AddressEntity extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 917822384733666966L;

    /* identifiers */
    /** Unique identifier of the address. */
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

    /** Owner of this address. */
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
    /** Street name and house number. */
    @Column(
            name = "street",
            columnDefinition = "TEXT",
            nullable = false
    )
    private String street;

    /** City name. */
    @Column(
            name = "city",
            columnDefinition = "VARCHAR(40)",
            nullable = false
    )
    private String city;

    /** Postal code of the address. */
    @Column(
            name = "zip_code",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String zipCode;

    /** Province, expressed as ISTAT code. */
    @Column(
            name = "province",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String province;

    /** State, expressed as ISTAT code. */
    @Column(
            name = "state",
            columnDefinition = "VARCHAR(10)",
            nullable = false
    )
    private String state;

}
