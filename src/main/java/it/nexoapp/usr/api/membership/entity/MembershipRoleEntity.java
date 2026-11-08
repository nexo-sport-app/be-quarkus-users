package it.nexoapp.usr.api.membership.entity;

import it.nexoapp.common.security.Role;
import it.nexoapp.common.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.UUID;

/**
 * One role held by a membership. A membership holds a set of roles; each role appears at most once per
 * membership. Tenant-owned: roles of one association are never visible under another.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "membership_role",
        uniqueConstraints = @UniqueConstraint(
                name = "membership_role_uq",
                columnNames = {"tenant_id", "membership_id", "role"}
        )
)
public class MembershipRoleEntity extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 6602178834410295581L;

    /* identifiers */
    /** Unique identifier of the role assignment. */
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

    /** Identifier of the membership holding the role. */
    @Column(
            name = "membership_id",
            columnDefinition = "UUID",
            nullable = false
    )
    private UUID membershipId;

    /* role data */
    /** The role held. */
    @Column(
            name = "role",
            columnDefinition = "VARCHAR(20)",
            nullable = false
    )
    @Enumerated(EnumType.STRING)
    private Role role;

}
