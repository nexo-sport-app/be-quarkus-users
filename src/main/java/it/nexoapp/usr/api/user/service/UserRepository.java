package it.nexoapp.usr.api.user.service;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.nexoapp.usr.api.user.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Persistence of {@link UserEntity}. Package-private: only the user services may use it.
 */
@ApplicationScoped
class UserRepository implements PanacheRepositoryBase<UserEntity, UUID> {

}
