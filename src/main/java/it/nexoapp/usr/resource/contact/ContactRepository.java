package it.nexoapp.usr.resource.contact;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.resource.contact.entity.ContactEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ContactRepository implements PanacheRepositoryBase<ContactEntity, UUID> {

}