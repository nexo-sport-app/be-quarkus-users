package it.nexoapp.usr.api.membership.entity;

import it.nexoapp.common.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * The belonging of a person to one association. It is tenant-owned: it is only ever visible inside the
 * association it belongs to. The roles of the membership live in {@link MembershipRoleEntity}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "membership")
public class MembershipEntity extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 3318462905571840273L;

    /* identifiers */
    /** Unique identifier of the membership. */
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

    /** Identifier of the person in the users register, if already registered (the member register comes later). */
    @Column(
            name = "user_id",
            columnDefinition = "UUID"
    )
    private UUID userId;

    /* state */
    /** Lifecycle status of the membership. */
    @Column(
            name = "status",
            columnDefinition = "VARCHAR(20)",
            nullable = false
    )
    @Enumerated(EnumType.STRING)
    private MembershipStatus status;

    // -----------------------------------------------------------------------------------------------------------------

    /** Lifecycle status of a membership. */
    public enum MembershipStatus {

        /** The member currently belongs to the association. */
        ACTIVE

    }

}
