package com.zipdamember.global.security.filter;

import com.zipdamember.global.security.oauth2.DelegatingOAuth2UserService;
import com.zipdamember.global.security.oauth2.OAuth2FailureHandler;
import com.zipdamember.global.security.oauth2.OAuth2SuccessHandler;
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
    public SecurityFilterChain filterChain(
            HttpSecurity httpSecurity,
            HeaderAuthenticationFilter headerAuthenticationFilter,
            DelegatingOAuth2UserService delegatingOAuth2UserService,
            OAuth2SuccessHandler oAuth2SuccessHandler,
            OAuth2FailureHandler oAuth2FailureHandler
    ) throws Exception {
        return httpSecurity
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 비활성화
            .httpBasic(AbstractHttpConfigurer::disable) // 화면 생성 비활성화
            .formLogin(AbstractHttpConfigurer::disable) // 폼로그인 기능 비활성화
            .csrf(AbstractHttpConfigurer::disable) // CSRF 토큰 인증 비활성화
            .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(request -> request.anyRequest().permitAll()) // 인증 여부와 무관하게 모든 요청 통과
            .oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/authorization"))
                    .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/callback/*"))
                    .userInfoEndpoint(userInfo -> userInfo.userService(delegatingOAuth2UserService))
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            )
            .build();
    }
}
