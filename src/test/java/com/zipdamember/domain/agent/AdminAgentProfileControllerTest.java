package com.zipdamember.domain.agent;

import com.zipdamember.domain.agent.controller.AdminAgentProfileController;
import com.zipdamember.domain.agent.service.AgentProfileService;
import com.zipdamember.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAgentProfileControllerTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean AgentProfileService service() { return mock(AgentProfileService.class); }
        @Bean AdminAgentProfileController controller(AgentProfileService service) {
            return new AdminAgentProfileController(service);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CS_ADMIN", "SALES_ADMIN", "SUPER_ADMIN"})
    void search_allowedAdminRole_returns200(String role) throws Exception {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            authenticate(role);
            var service = context.getBean(AgentProfileService.class);
            var mvc = MockMvcBuilders.standaloneSetup(context.getBean(AdminAgentProfileController.class))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();
            mvc.perform(get("/api/member/admin/agencies"))
                    .andExpect(status().isOk());
            verify(service).search(any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SYSTEM", "USER", "AGENT"})
    void search_disallowedRole_returns403(String role) throws Exception {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            authenticate(role);
            var service = context.getBean(AgentProfileService.class);
            var mvc = MockMvcBuilders.standaloneSetup(context.getBean(AdminAgentProfileController.class))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();

            mvc.perform(get("/api/member/admin/agencies"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
