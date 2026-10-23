package com.connectpro.auth.repository;

import com.connectpro.auth.entity.UserCredential;
import com.connectpro.auth.model.CredentialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

    Optional<UserCredential> findByUserIdAndCredentialType(
            UUID userId,
            CredentialType credentialType
    );
}
