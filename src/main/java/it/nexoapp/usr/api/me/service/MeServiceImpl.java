package it.nexoapp.usr.api.me.service;

import io.quarkus.security.identity.SecurityIdentity;
import it.nexoapp.common.security.NexoPrincipal;
import it.nexoapp.usr.api.me.dto.MeResponse;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Default {@link MeService}. It reads only the {@link NexoPrincipal} attached to the caller's own security
 * identity: no client input selects another identity.
 */
@ApplicationScoped
@RequiredArgsConstructor
class MeServiceImpl implements MeService {

    /* - security - */
    private final SecurityIdentity identity;

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public MeResponse whoAmI() {
        final NexoPrincipal principal = identity.getAttribute(NexoPrincipal.ATTRIBUTE);
        if (principal == null || principal.state() != NexoPrincipal.LinkState.LINKED) {
            return new MeResponse(false, null, null, Set.of());
        }
        return new MeResponse(true, principal.associationId(), principal.memberId(), principal.roles());
    }

}
