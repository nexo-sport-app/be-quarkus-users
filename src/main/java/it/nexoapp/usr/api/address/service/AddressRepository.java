package it.nexoapp.usr.api.address.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.api.address.entity.AddressEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Persistence of {@link AddressEntity}. Package-private: only the address services may use it.
 */
@ApplicationScoped
class AddressRepository implements PanacheRepositoryBase<AddressEntity, UUID> {

}
