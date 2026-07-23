package com.aivle13.fin_audit_ai.domain.user.repository;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailAndName(
            String email,
            String name
    );

    Optional<UserEntity> findByEmail(String email);
}