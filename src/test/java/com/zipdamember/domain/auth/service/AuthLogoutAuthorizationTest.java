package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.controller.AuthController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthLogoutAuthorizationTest {
    private AnnotationConfigApplicationContext context;
    private AuthController controller;
    private AuthService service;

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean AuthService authService() { return mock(AuthService.class); }
        @Bean AuthController authController(AuthService service) { return new AuthController(service); }
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        controller = context.getBean(AuthController.class);
        service = context.getBean(AuthService.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "AGENT"})
    void memberRolesCanLogout(String role) {
        var authentication = new UsernamePasswordAuthenticationToken("1", null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        assertThat(controller.logout(request, response, authentication).getStatusCode().value()).isEqualTo(200);
        verify(service).logout(request, response, 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "CS_ADMIN", "SALES_ADMIN"})
    void nonMemberRolesCannotLogout(String role) {
        var authentication = new UsernamePasswordAuthenticationToken("1", null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertThatThrownBy(() -> controller.logout(new MockHttpServletRequest(), new MockHttpServletResponse(), authentication))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void unauthenticatedRequestCannotLogout() {
        assertThatThrownBy(() -> controller.logout(new MockHttpServletRequest(), new MockHttpServletResponse(), null))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(service);
    }
}
