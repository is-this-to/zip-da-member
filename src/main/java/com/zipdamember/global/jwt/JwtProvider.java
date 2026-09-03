package com.zipdamember.global.jwt;

import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.global.error.custom.business.InvalidTokenException;
import com.zipdamember.global.jwt.request.AdminTokenGenerateRequest;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.secret()));
    }

    // ADMIN 토큰 관련
    public String generateAdminAccessToken(AdminTokenGenerateRequest request) {
        Date now = new Date();
        // 복수 역할 가능
        List<String> roles = request.roles().stream()
            .map(Enum::name)
            .toList();

        return Jwts.builder()
            .header()
            .type(jwtConfig.type())
            .and()
            .subject(String.valueOf(request.adminId()))
            .issuer(jwtConfig.issuer())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + jwtConfig.adminAccessTokenExpiryMs()))
            .claim("type", "ADMIN")
            .claim("tokenType", "ACCESS")
            .claim("roles", roles)
            .signWith(secretKey)
            .compact();
    }

    public String generateAdminRefreshToken(AdminTokenGenerateRequest request) {
        Date now = new Date();

        return Jwts.builder()
            .header()
            .type(jwtConfig.type())
            .and()
            .subject(String.valueOf(request.adminId()))
            .issuer(jwtConfig.issuer())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + jwtConfig.refreshTokenExpiryMs()))
            .claim("type", "ADMIN")
            .claim("tokenType", "REFRESH")
            .signWith(secretKey)
            .compact();
    }

    // MEMBER 토큰 관련
    public String generateAccessToken(MemberAccount member) {
        return this.generateToken(member, jwtConfig.accessTokenExpiryMs());
    }

    public String generateRefreshToken(MemberAccount member) {
        return this.generateToken(member, jwtConfig.refreshTokenCookieMaxAgeSeconds());
    }

    private String generateToken(MemberAccount member, int expiry) {
        Date now = new Date();

        return Jwts.builder()
            .header() // 헤더를 셋팅하겠다.
            .type(jwtConfig.type()) // 토큰 유형
            .and()
            .subject(String.valueOf(member.getMemberId())) // sub 셋팅
            .issuer(jwtConfig.issuer()) // 토큰 발급자 셋팅
            .issuedAt(now) // 토급 발급시간 설정
            .expiration(new Date(now.getTime() + expiry)) // 토큰 만료 시간 설정
            .claim("role", member.getMemberRole()) // Private Claim 설정
            .signWith(secretKey) // 시그니쳐 작성
            .compact();
    }

    // JWT 토큰을 검증, Claims 정보 추출
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(this.secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                ;
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("토큰이 만료됐습니다.");
        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException("서명이 위조된 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("토큰 검증에 실패했습니다.");
        }
    }
}
