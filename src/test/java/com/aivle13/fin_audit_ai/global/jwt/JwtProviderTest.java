package com.aivle13.fin_audit_ai.global.jwt;

import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.user.auth.ExpiredTokenException;
import com.aivle13.fin_audit_ai.global.exception.user.auth.InvalidTokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class JwtProviderTest {

    private static final String SECRET = "test-jwt-secret-must-be-at-least-32-bytes-long!!";
    private static final long ACCESS_VALIDITY = 60_000L;
    private static final long REFRESH_VALIDITY = 120_000L;
    private static final long REMEMBER_ME_REFRESH_VALIDITY = 1_209_600_000L; // 14일

    private JwtProvider newProvider() {
        return newProvider(SECRET);
    }

    private JwtProvider newProvider(String secret) {
        JwtProperties properties = new JwtProperties(
                secret, ACCESS_VALIDITY, REFRESH_VALIDITY, REMEMBER_ME_REFRESH_VALIDITY
        );
        return new JwtProvider(properties);
    }

    @Test
    void 액세스_토큰을_생성하면_사용자ID와_권한을_파싱할_수_있다() {
        JwtProvider provider = newProvider();

        String token = provider.createAccessToken(1L, UserRole.ADMIN);

        assertThat(provider.getUserId(token)).isEqualTo(1L);
        assertThat(provider.getRole(token)).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void 액세스_토큰은_액세스_토큰_검증을_통과한다() {
        JwtProvider provider = newProvider();
        String token = provider.createAccessToken(1L, UserRole.USER);

        assertThatCode(() -> provider.validateAccessToken(token))
                .doesNotThrowAnyException();
    }

    @Test
    void 리프레시_토큰으로_액세스_토큰_검증을_시도하면_예외() {
        JwtProvider provider = newProvider();
        String refreshToken = provider.createRefreshToken(1L, UserRole.USER, false);

        assertThatThrownBy(() -> provider.validateAccessToken(refreshToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void 액세스_토큰으로_리프레시_토큰_검증을_시도하면_예외() {
        JwtProvider provider = newProvider();
        String accessToken = provider.createAccessToken(1L, UserRole.USER);

        assertThatThrownBy(() -> provider.validateRefreshToken(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void 리프레시_토큰은_리프레시_토큰_검증을_통과한다() {
        JwtProvider provider = newProvider();
        String refreshToken = provider.createRefreshToken(1L, UserRole.USER, false);

        assertThatCode(() -> provider.validateRefreshToken(refreshToken))
                .doesNotThrowAnyException();
    }

    @Test
    void rememberMe가_true면_기억하기_유효기간이_적용된다() {
        JwtProvider provider = newProvider();

        String token = provider.createRefreshToken(1L, UserRole.USER, true);

        assertThat(provider.getRememberMe(token)).isTrue();
        assertThat(provider.getRemainingValidity(token))
                .isCloseTo(REMEMBER_ME_REFRESH_VALIDITY, within(3_000L));
    }

    @Test
    void rememberMe가_false면_일반_리프레시_유효기간이_적용된다() {
        JwtProvider provider = newProvider();

        String token = provider.createRefreshToken(1L, UserRole.USER, false);

        assertThat(provider.getRememberMe(token)).isFalse();
        assertThat(provider.getRemainingValidity(token))
                .isCloseTo(REFRESH_VALIDITY, within(3_000L));
    }

    @Test
    void 액세스_토큰의_남은_유효시간을_밀리초로_반환한다() {
        JwtProvider provider = newProvider();

        String token = provider.createAccessToken(1L, UserRole.USER);

        assertThat(provider.getRemainingValidity(token))
                .isCloseTo(ACCESS_VALIDITY, within(3_000L));
    }

    @Test
    void 만료된_토큰을_파싱하면_ExpiredTokenException() {
        JwtProperties expiredProperties = new JwtProperties(
                SECRET, -1_000L, REFRESH_VALIDITY, REMEMBER_ME_REFRESH_VALIDITY
        );
        JwtProvider provider = new JwtProvider(expiredProperties);

        String alreadyExpiredToken = provider.createAccessToken(1L, UserRole.USER);

        assertThatThrownBy(() -> provider.getUserId(alreadyExpiredToken))
                .isInstanceOf(ExpiredTokenException.class);
    }

    @Test
    void 다른_키로_서명된_토큰을_파싱하면_InvalidTokenException() {
        JwtProvider provider = newProvider();
        String forgedToken = tokenSignedWithDifferentKey();

        assertThatThrownBy(() -> provider.getUserId(forgedToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void 형식이_깨진_토큰을_파싱하면_InvalidTokenException() {
        JwtProvider provider = newProvider();

        assertThatThrownBy(() -> provider.getUserId("this-is-not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    private String tokenSignedWithDifferentKey() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "other-jwt-secret-must-be-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8)
        );
        Date now = new Date();

        return Jwts.builder()
                .subject("1")
                .claim("role", UserRole.USER.name())
                .claim("typ", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_VALIDITY))
                .signWith(otherKey)
                .compact();
    }
}
