package com.zipdamember.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String userType = request.getHeader("X-User-Type");
        String userRole = request.getHeader("X-User-Role");

        if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(userRole)) {
            // 일반사용자 단일 역할 X-User-Role: USER
            // 관리자 단일 역할 X-User-Role: CS_ADMIN
            // 관리자 다중 역할 X-User-Role: CS_ADMIN,SALES_ADMIN
            List<SimpleGrantedAuthority> authorities =
                Arrays.stream(userRole.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .distinct()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                //List.of(new SimpleGrantedAuthority("ROLE_" + userRole))
                authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}