package it.nexoapp.usr.api.privacyconsent.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.api.privacyconsent.entity.PrivacyConsentEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Persistence of {@link PrivacyConsentEntity}. Package-private: only the privacy consent services may use it.
 */
@ApplicationScoped
class PrivacyConsentRepository implements PanacheRepositoryBase<PrivacyConsentEntity, UUID> {

}
