package it.nexoapp.usr.api.contact.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.api.contact.entity.ContactEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Persistence of {@link ContactEntity}. Package-private: only the contact services may use it.
 */
@ApplicationScoped
class ContactRepository implements PanacheRepositoryBase<ContactEntity, UUID> {

}
