package com.zipdamember.domain.member;

import com.zipdamember.domain.member.controller.AdminMemberController;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.AdminMemberRepository;
import com.zipdamember.domain.member.request.AdminMemberSearchRequest;
import com.zipdamember.domain.member.response.AdminMemberResponse;
import com.zipdamember.domain.member.service.AdminMemberService;
import com.zipdamember.global.error.GlobalExceptionHandler;
import com.zipdamember.global.config.jpa.JPAWithDeleted;
import com.zipdamember.global.config.jpa.JPAWithDeletedAspect;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminMemberSearchTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean AdminMemberService service() { return mock(AdminMemberService.class); }
        @Bean AdminMemberController controller(AdminMemberService service) {
            return new AdminMemberController(service);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CS_ADMIN", "SUPER_ADMIN", "SALES_ADMIN,CS_ADMIN"})
    void search_allowedRole_returns200(String roles) throws Exception {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            authenticate(roles);
            var mvc = MockMvcBuilders.standaloneSetup(context.getBean(AdminMemberController.class))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();
            mvc.perform(get("/api/member/admin/members")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("00"));
            verify(context.getBean(AdminMemberService.class)).search(
                    new AdminMemberSearchRequest(null, null, null, 0, 20));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SALES_ADMIN", "USER", "AGENT", ""})
    void search_forbiddenRole_returns403(String roles) throws Exception {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            authenticate(roles);
            var mvc = MockMvcBuilders.standaloneSetup(context.getBean(AdminMemberController.class))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();
            mvc.perform(get("/api/member/admin/members")).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("E04"));
            verifyNoInteractions(context.getBean(AdminMemberService.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101", "-1"})
    void search_invalidSize_returns400(String size) throws Exception {
        var service = mock(AdminMemberService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminMemberController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/member/admin/members").param("size", size))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void search_invalidStatus_returns400() throws Exception {
        var service = mock(AdminMemberService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminMemberController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/member/admin/members").param("status", "INVALID"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void search_defaultsAndLiteralPattern_usesFixedSort() {
        var repository = mock(AdminMemberRepository.class);
        when(repository.findMembers(any(), any(), any(), any())).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(3);
            assertThat(pageable.getPageSize()).isEqualTo(20);
            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getSort()).isEqualTo(
                    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("memberId")));
            return new PageImpl<MemberAccount>(List.of(), pageable, 0);
        });
        var result = new AdminMemberService(repository).search(
                new AdminMemberSearchRequest("  a%_!  ", null, null, null, null));
        verify(repository).findMembers(eq("%a!%!_!!%"), isNull(), isNull(), any());
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void search_blankKeyword_omitsKeywordCondition() {
        var repository = mock(AdminMemberRepository.class);
        when(repository.findMembers(isNull(), any(), any(), any()))
                .thenAnswer(i -> new PageImpl<MemberAccount>(List.of(), i.getArgument(3), 0));
        new AdminMemberService(repository).search(
                new AdminMemberSearchRequest("   ", null, null, 1, 100));
        verify(repository).findMembers(isNull(), isNull(), isNull(),
                argThat(p -> p.getPageNumber() == 1 && p.getPageSize() == 100));
    }

    @Test
    void response_personalData_isMaskedAndIdIsString() {
        var member = new MemberAccount();
        member.setMemberId(782345678901230101L);
        member.setName("홍길동");
        member.setEmail("hong@mail.com");
        var result = AdminMemberResponse.from(member);
        assertThat(result.memberId()).isEqualTo("782345678901230101");
        assertThat(result.name()).isEqualTo("홍*동");
        assertThat(result.email()).isEqualTo("ho***@mail.com");
        member.setName("김");
        member.setEmail("a@mail.com");
        assertThat(AdminMemberResponse.from(member).name()).isEqualTo("*");
        assertThat(AdminMemberResponse.from(member).email()).isEqualTo("***@mail.com");
    }

    @Test
    void withDeleted_exception_restoresEnabledFilter() throws Throwable {
        var entityManager = mock(EntityManager.class);
        var session = mock(Session.class);
        var point = mock(ProceedingJoinPoint.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getEnabledFilter("softDelete")).thenReturn(mock(org.hibernate.Filter.class));
        var annotation = AdminMemberRepository.class.getMethod("findMembers", String.class,
                com.zipdamember.domain.member.constant.MemberStatus.class,
                com.zipdamember.global.security.constant.MemberRolePolicy.class,
                Pageable.class).getAnnotation(JPAWithDeleted.class);
        when(point.proceed()).thenThrow(new IllegalStateException("query failed"));
        assertThatThrownBy(() -> new JPAWithDeletedAspect(entityManager)
                .executeWithOutFiltering(point, annotation)).isInstanceOf(IllegalStateException.class);
        var order = inOrder(session, point);
        order.verify(session).getEnabledFilter("softDelete");
        order.verify(session).disableFilter("softDelete");
        order.verify(point).proceed();
        order.verify(session).enableFilter("softDelete");
    }

    private void authenticate(String roles) {
        var authorities = roles.isEmpty() ? List.<SimpleGrantedAuthority>of()
                : Arrays.stream(roles.split(",")).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, authorities));
    }
}
