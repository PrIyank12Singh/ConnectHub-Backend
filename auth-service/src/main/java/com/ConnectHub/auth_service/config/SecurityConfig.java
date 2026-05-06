package com.connecthub.auth_service.config;

import com.connecthub.auth_service.dto.AuthResponse;
import com.connecthub.auth_service.service.GoogleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    private final GoogleAuthService googleAuthService;

    public SecurityConfig(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/error", "/login/**", "/oauth2/**","/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler())
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication) throws IOException {

                // 1. Get OAuth2 user from Spring Security context
                OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

                // 2. Generate our own JWT via GoogleAuthService
                AuthResponse authResponse = googleAuthService.handleGoogleLogin(oauthUser);

                // 3. URL-encode all values safely
                String token    = URLEncoder.encode(authResponse.getAccessToken(),         StandardCharsets.UTF_8);
                String userId   = URLEncoder.encode(authResponse.getUserId().toString(),   StandardCharsets.UTF_8);
                String username = URLEncoder.encode(authResponse.getUsername(),            StandardCharsets.UTF_8);
                String email    = URLEncoder.encode(authResponse.getEmail(),               StandardCharsets.UTF_8);
                String role     = URLEncoder.encode(authResponse.getRole().name(),         StandardCharsets.UTF_8);
                String redirectUrl = "http://localhost:4200/oauth-callback"
                        + "?token="    + token
                        + "&userId="   + userId
                        + "&username=" + username
                        + "&email="    + email
                        + "&role="     + role;

                response.sendRedirect(redirectUrl);
            }
        };
    }
}

