package it.nexoapp.usr.resource.privacyconsent;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.resource.privacyconsent.entity.PrivacyConsentEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class PrivacyConsentRepository implements PanacheRepositoryBase<PrivacyConsentEntity, UUID> {

}