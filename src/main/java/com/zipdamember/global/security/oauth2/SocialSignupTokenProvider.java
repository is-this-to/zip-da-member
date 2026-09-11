package com.zipdamember.global.security.oauth2;

import com.zipdamember.domain.auth.model.SocialSignupClaims;
import com.zipdamember.global.config.oauth.OAuth2Config;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.jwt.JwtConfig;
import com.zipdamember.global.security.constant.ProviderPolicy;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class SocialSignupTokenProvider {

    private static final String TOKEN_TYPE = "SOCIAL_SIGNUP";

    private final OAuth2Config oAuth2Config;
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public SocialSignupTokenProvider(OAuth2Config oAuth2Config, JwtConfig jwtConfig) {
        this.oAuth2Config = oAuth2Config;
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(decodeSecret(jwtConfig.secret()));
    }

    public String generate(SocialSignupClaims signupClaims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + oAuth2Config.socialSignupTokenExpirySeconds() * 1000L);

        return Jwts.builder()
                .header().type(jwtConfig.type()).and()
                .subject(signupClaims.providerUserId())
                .issuer(jwtConfig.issuer())
                .issuedAt(now)
                .expiration(expiration)
                .claim("type", TOKEN_TYPE)
                .claim("provider", signupClaims.provider().name())
                .claim("email", signupClaims.email())
                .claim("nickname", signupClaims.nickname())
                .claim("profileImageUrl", signupClaims.profileImageUrl())
                .signWith(secretKey)
                .compact();
    }

    public SocialSignupClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TOKEN_TYPE.equals(claims.get("type", String.class))) {
                throw new InvalidTokenException("소셜 회원가입 토큰이 아닙니다.");
            }

            return new SocialSignupClaims(
                    ProviderPolicy.valueOf(claims.get("provider", String.class)),
                    claims.getSubject(),
                    requireClaim(claims, "email"),
                    requireClaim(claims, "nickname"),
                    claims.get("profileImageUrl", String.class)
            );
        } catch (ExpiredJwtException exception) {
            throw new InvalidTokenException("소셜 회원가입 정보가 만료되었습니다. 카카오 로그인을 다시 진행해주세요.");
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException("유효하지 않은 소셜 회원가입 정보입니다.");
        }
    }

    private String requireClaim(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (value == null || value.isBlank()) {
            throw new InvalidTokenException("소셜 회원가입 필수 정보가 없습니다.");
        }
        return value;
    }

    private byte[] decodeSecret(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (DecodingException exception) {
            return Decoders.BASE64URL.decode(secret);
        }
    }
}
