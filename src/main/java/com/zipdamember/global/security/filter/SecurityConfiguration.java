package com.zipdamember.global.security.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Security
@EnableMethodSecurity // 메서드 레벨 권한 제어 활성화 -> 기본적으로 모든 요청을 다 허용
@RequiredArgsConstructor
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity, HeaderAuthenticationFilter headerAuthenticationFilter) {
        return httpSecurity
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // 화면 생성 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // 폼로그인 기능 비활성화
                .csrf(AbstractHttpConfigurer::disable) // CSRF 토큰 인증 비활성화
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(request -> request.anyRequest().permitAll()) // 인증 여부와 무관하게 모든 요청 통과
//               TODO: 추후 2Oauth 추가
//                .oauth2Login(oauth2 ->
//                        oauth2.authorizationEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/authorization")) // 기본 경로 설정
//                                .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/callback/*")) // 리다이렉트 경로 설정
//                                .userInfoEndpoint(userInfo -> userInfo.userService(delegatingOAuth2UserService)) // provider routing 처리를 할 서비스 등록
//                                .successHandler(oAuth2SuccessHandler) // 성공 핸들러 등록
//                                .failureHandler(oAuthFailureHandler) // 실패 핸들러 등록
//                )
                .build();
    }
}
