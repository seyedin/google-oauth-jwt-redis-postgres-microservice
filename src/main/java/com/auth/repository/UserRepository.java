package com.auth.repository;

import com.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * This interface is the user repository.
 * It helps to work with the user table.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * This method checks if username is already used.
     *
     * @param username the username of the user
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    /**
     * This method finds user by email and provider.
     */
    Optional<User> findByEmailAndProvider(String email, String provider);

}
