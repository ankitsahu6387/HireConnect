package com.hireconnect.auth.repository;

import java.util.Optional;

import com.hireconnect.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<UserCredential, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<UserCredential> findByEmailIgnoreCase(String email);
}
