package com.aivle13.fin_audit_ai.domain.user.repository;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
