package it.nexoapp.usr.api.membership.entity;

import it.nexoapp.common.hibernate.entity.AuditEntity;
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
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Link from an external identity (issuer and subject of the OIDC token, and nothing else) to a membership.
 * It is a global table, not a tenant entity: it is read before the association of the caller is known, so
 * it is read only under {@code TenantContext.callUnscoped}. The tenant identifier is a plain data column
 * saying which association the linked membership belongs to.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "identity_link",
        uniqueConstraints = @UniqueConstraint(
                name = "identity_link_uq",
                columnNames = {"issuer", "subject", "membership_id"}
        )
)
public class IdentityLinkEntity extends AuditEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1947305528164073392L;

    /* identifiers */
    /** Unique identifier of the link. */
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

    /* external identity */
    /** Issuer of the token ({@code iss} claim). */
    @Column(
            name = "issuer",
            columnDefinition = "TEXT",
            nullable = false
    )
    private String issuer;

    /** Subject of the token ({@code sub} claim), unique within the issuer. */
    @Column(
            name = "subject",
            columnDefinition = "TEXT",
            nullable = false
    )
    private String subject;

    /* linked membership */
    /** Identifier of the membership the identity is linked to. */
    @Column(
            name = "membership_id",
            columnDefinition = "UUID",
            nullable = false
    )
    private UUID membershipId;

    /** Identifier of the association owning the linked membership (plain data, not a tenant filter). */
    @Column(
            name = "tenant_id",
            columnDefinition = "UUID",
            nullable = false,
            updatable = false
    )
    private UUID tenantId;

    /** Instant the identity was linked to the membership. */
    @Column(
            name = "linked_at",
            columnDefinition = "TIMESTAMPTZ",
            nullable = false
    )
    private OffsetDateTime linkedAt;

}
