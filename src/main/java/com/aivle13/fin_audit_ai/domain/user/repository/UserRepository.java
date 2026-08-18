package com.aivle13.fin_audit_ai.domain.user.repository;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * 만료된 시연용 게스트 계정의 id.
     *
     * <p>게스트는 이메일 형태로 구분한다({@code DemoAccountService}). 별도 컬럼이나 역할을
     * 두면 실제 사용자 스키마까지 시연 기능이 침범한다.
     */
    @Query("""
            SELECT u.id FROM UserEntity u
            WHERE u.email LIKE :emailPattern
              AND u.createdAt < :expiredBefore
            ORDER BY u.id
            """)
    List<Long> findExpiredGuestIds(
            @Param("emailPattern") String emailPattern,
            @Param("expiredBefore") LocalDateTime expiredBefore,
            Limit limit
    );
}