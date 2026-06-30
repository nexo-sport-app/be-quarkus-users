package it.nexoapp.usr.resource.address;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.resource.address.entity.AddressEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class AddressRepository implements PanacheRepositoryBase<AddressEntity, UUID> {

}