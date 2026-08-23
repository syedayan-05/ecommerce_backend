package com.ayan.ecommerce.config;

import com.ayan.ecommerce.security.CustomAccessDeniedHandler;
import com.ayan.ecommerce.security.CustomAuthenticationEntryPoint;
import com.ayan.ecommerce.security.GoogleOAuth2SuccessHandler;
import com.ayan.ecommerce.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // CSRF
                .csrf(csrf -> csrf.disable())

                // SESSION
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // AUTHORIZATION
                .authorizeHttpRequests(auth -> auth

                        // ==============================
                        // PUBLIC
                        // ==============================

                        .requestMatchers(
                                "/auth/**",
                                "/test-email",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()


                        // ==============================
                        // PUBLIC CATEGORY GET
                        // ==============================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/categories",
                                "/categories/**"
                        ).permitAll()


                        // ==============================
                        // PUBLIC PRODUCT GET
                        // ==============================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/products",
                                "/products/**"
                        ).permitAll()


                        // ==============================
                        // ADMIN PRODUCT
                        // ==============================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/products",
                                "/products/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products",
                                "/products/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/products",
                                "/products/**"
                        ).hasRole("ADMIN")


                        // ==============================
                        // ADMIN CATEGORY
                        // ==============================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/categories",
                                "/categories/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/categories",
                                "/categories/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/categories",
                                "/categories/**"
                        ).hasRole("ADMIN")


                        // ==============================
                        // PAYMENTS
                        // ==============================

                        .requestMatchers(
                                "/api/payments/**"
                        ).hasAnyRole("USER", "ADMIN")


                        // ==============================
                        // USERS
                        // ==============================

                        .requestMatchers(
                                "/users/**"
                        ).hasAnyRole("USER", "ADMIN")


                        // ==============================
                        // ADDRESSES
                        // ==============================

                        .requestMatchers(
                                "/addresses/**"
                        ).hasAnyRole("USER", "ADMIN")


                        // ==============================
                        // EVERYTHING ELSE
                        // ==============================

                        .anyRequest().authenticated()
                )

                // GOOGLE OAUTH2
                .oauth2Login(oauth ->
                        oauth.successHandler(
                                googleOAuth2SuccessHandler
                        )
                )

                // EXCEPTION HANDLING
                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )

                        .accessDeniedHandler(
                                accessDeniedHandler
                        )
                )

                // JWT FILTER
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

