package com.example.scanlogin.security;

import com.example.scanlogin.infrastructure.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, AuthCookies cookies,
                                    UserRepository users) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.csrfTokenRepository(csrf)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // All POSTs are protected by bearer tokens, captchas or anonymous
                        // ticket credentials instead; ignoring CSRF everywhere also avoids
                        // cross-tab XSRF-cookie races between the two frontends.
                        .ignoringRequestMatchers("/api/mobile/**", "/api/auth/qr/scan", "/api/auth/qr/confirm",
                                "/api/auth/qr/create", "/api/auth/qr/consume", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout"))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/auth/qr/status", "/api/auth/captcha").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/mobile/register",
                                "/api/mobile/login", "/api/mobile/refresh",
                                "/api/auth/qr/create", "/api/auth/login", "/api/auth/refresh",
                                "/api/auth/logout").permitAll()
                        // The admin API is guarded by the X-Admin-Key header inside the
                        // controller, not by user authentication.
                        .requestMatchers("/api/admin/**").permitAll()
                        .requestMatchers("/api/mobile/**", "/api/auth/qr/scan", "/api/auth/qr/confirm")
                        .hasRole("MOBILE")
                        .requestMatchers("/api/auth/me").hasRole("PC")
                        .requestMatchers(HttpMethod.POST, "/api/auth/qr/consume").permitAll()
                        .anyRequest().denyAll())
                // Unauthenticated calls must answer 401 so the PC client can rotate its
                // refresh cookie; Spring's default would be a bare 403.
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, cookies, users),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
