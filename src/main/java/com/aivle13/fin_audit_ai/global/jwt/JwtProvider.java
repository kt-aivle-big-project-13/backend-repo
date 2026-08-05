package com.aivle13.fin_audit_ai.global.jwt;

import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.user.auth.ExpiredTokenException;
import com.aivle13.fin_audit_ai.global.exception.user.auth.InvalidTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_REMEMBER_ME = "rememberMe";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties jwtProperties;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String createAccessToken(Long userId, UserRole role) {
        return createToken(userId, role, TYPE_ACCESS, jwtProperties.accessTokenValidity(), false);
    }

    public String createRefreshToken(
            Long userId,
            UserRole role,
            boolean rememberMe
    ) {
        long validityMillis = rememberMe
                ? jwtProperties.rememberMeRefreshTokenValidity()
                : jwtProperties.refreshTokenValidity();

        return createToken(
                userId,
                role,
                TYPE_REFRESH,
                validityMillis,
                rememberMe
        );
    }

    // 액세스 토큰인지 검증 (리프레시 토큰을 인가 헤더에 넣어 쓰는 것을 차단)
    public void validateAccessToken(String token) {
        validateTokenType(token, TYPE_ACCESS);
    }

    // 리프레시 토큰인지 검증 (액세스 토큰으로 재발급을 시도하는 것을 차단)
    public void validateRefreshToken(String token) {
        validateTokenType(token, TYPE_REFRESH);
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public UserRole getRole(String token) {
        return UserRole.valueOf(parseClaims(token).get(CLAIM_ROLE, String.class));
    }

    public boolean getRememberMe(String token) {
        Boolean rememberMe = parseClaims(token).get(
                CLAIM_REMEMBER_ME,
                Boolean.class
        );

        return Boolean.TRUE.equals(rememberMe);
    }

    // 남은 유효시간(ms). 블랙리스트 TTL을 토큰 잔여 수명과 맞추기 위해 사용.
    public long getRemainingValidity(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    private String createToken(
            Long userId,
            UserRole role,
            String type,
            long validityMillis,
            boolean rememberMe
    ) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_REMEMBER_ME, rememberMe)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    private void validateTokenType(String token, String expectedType) {
        String actualType = parseClaims(token).get(CLAIM_TYPE, String.class);

        if (!expectedType.equals(actualType)) {
            throw new InvalidTokenException();
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException ex) {
            throw new ExpiredTokenException();

        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException();
        }
    }
}