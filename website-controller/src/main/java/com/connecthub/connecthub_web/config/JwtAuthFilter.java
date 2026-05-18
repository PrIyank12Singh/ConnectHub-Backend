package com.connecthub.connecthub_web.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Also accept X-User-Id forwarded by api-gateway (already validated)
        String userIdFromGateway = request.getHeader("X-User-Id");
        String roleFromGateway   = request.getHeader("X-User-Role");

        if (userIdFromGateway != null && roleFromGateway != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Request came through api-gateway — trust forwarded headers
            setAuthentication(userIdFromGateway, roleFromGateway);
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String userId = jwtUtil.extractUserId(token);
                String role   = jwtUtil.extractRole(token);
                setAuthentication(userId, role);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String userId, String role) {
        String springRole = role != null && !role.startsWith("ROLE_") ? "ROLE_" + role : role;
        var authorities = List.of(new SimpleGrantedAuthority(springRole != null ? springRole : "ROLE_USER"));
        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}


