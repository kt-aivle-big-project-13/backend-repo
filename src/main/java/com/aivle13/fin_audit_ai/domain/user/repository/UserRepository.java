package com.aivle13.fin_audit_ai.domain.user.repository;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 비밀번호 찾기: 이메일과 이름이 일치하는 사용자 조회
    Optional<UserEntity> findByEmailAndName(
            String email,
            String name
    );

    // 법령 개정 알림 등 전체 활성 사용자 대상 브로드캐스트에 사용
    List<UserEntity> findByIsActiveTrue();

    // 회원가입: 이메일 중복 검사
    boolean existsByEmail(String email);

    // 로그인: 이메일로 사용자 조회
    Optional<UserEntity> findByEmail(String email);
}