package it.nexoapp.usr.api.membership.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.api.membership.entity.IdentityLinkEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Persistence of {@link IdentityLinkEntity}. Package-private: only the membership services use it.
 * The table is global (not a tenant entity), so its queries see the links of every association, inside any
 * tenant scope as well as unscoped.
 */
@ApplicationScoped
class IdentityLinkRepository implements PanacheRepositoryBase<IdentityLinkEntity, UUID> {

    /**
     * Finds the links of one external identity.
     *
     * @param issuer  issuer of the token ({@code iss})
     * @param subject subject of the token ({@code sub})
     * @return the links of the identity, one per linked membership, empty when it is not linked
     */
    List<IdentityLinkEntity> findByIssuerAndSubject(final String issuer, final String subject) {
        return list("issuer = ?1 and subject = ?2", issuer, subject);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether any of the given memberships is already linked to some identity.
     *
     * @param membershipIds identifiers of the memberships to check
     * @return {@code true} when at least one of them has a link, {@code false} when none has or the collection is empty
     */
    boolean existsForAnyMembership(final Collection<UUID> membershipIds) {
        if (membershipIds.isEmpty()) {
            return false;
        }
        return count("membershipId in ?1", membershipIds) > 0;
    }

}
