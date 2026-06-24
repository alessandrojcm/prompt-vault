package com.promptvault.api.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByUsernameNormalized(String usernameNormalized);

    boolean existsByEmailAddressNormalized(String emailAddressNormalized);

    Optional<UserEntity> findByUsernameNormalized(String usernameNormalized);

    List<UserEntity> findAllByOrderByUsernameAsc();

    List<UserEntity> findAllByRoleOrderByUsernameAsc(Role role);
}
