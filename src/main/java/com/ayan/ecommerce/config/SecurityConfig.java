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

                // =============================================
                // CSRF
                // =============================================

                .csrf(csrf -> csrf.disable())


                // =============================================
                // SESSION
                // JWT Based Authentication
                // =============================================

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                // =============================================
                // AUTHORIZATION
                // =============================================

                .authorizeHttpRequests(auth -> auth

                        // =============================================
                        // PUBLIC APIs
                        // =============================================

                        .requestMatchers(
                                "/auth/**",
                                "/test-email",

                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",

                                // Google OAuth2
                                "/oauth2/**",
                                "/login/**",

                                // Razorpay Webhook
                                "/api/webhooks/razorpay"
                        ).permitAll()


                        // =============================================
                        // PUBLIC CATEGORY APIs
                        // Anyone can view categories
                        // =============================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/categories",
                                "/categories/**"
                        ).permitAll()


                        // =============================================
                        // PUBLIC PRODUCT APIs
                        // Anyone can view products
                        // =============================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/products",
                                "/products/**"
                        ).permitAll()


                        // =============================================
                        // ADMIN PRODUCT APIs
                        // =============================================

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


                        // =============================================
                        // ADMIN CATEGORY APIs
                        // =============================================

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


                        // =============================================
                        // CART
                        // USER + ADMIN
                        // =============================================

                        .requestMatchers(
                                "/cart/**"
                        ).hasAnyRole("USER", "ADMIN")


                        // =============================================
                        // ORDERS
                        // =============================================

                        // ---------------------------------------------
                        // ADMIN ORDER APIs
                        // IMPORTANT:
                        // Keep these BEFORE generic /orders/**
                        // ---------------------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/admin/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/orders/admin/**"
                        ).hasRole("ADMIN")


                        // ---------------------------------------------
                        // CUSTOMER ORDER APIs
                        // ---------------------------------------------

                        .requestMatchers(
                                HttpMethod.POST,
                                "/orders/checkout"
                        ).hasRole("USER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/my-orders"
                        ).hasRole("USER")


                        // Customer + Admin can access order details
                        // Ownership is checked in service layer
                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/**"
                        ).hasAnyRole("USER", "ADMIN")


                        // Customer order cancellation
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/orders/**"
                        ).hasRole("USER")


                        // =============================================
                        // PAYMENTS
                        // USER + ADMIN
                        // =============================================

                        .requestMatchers(
                                "/api/payments/**"
                        ).hasAnyRole("USER", "ADMIN")


                        // =============================================
                        // USERS
                        // USER + ADMIN
                        // =============================================

                        .requestMatchers(
                                "/users/**"
                        ).hasAnyRole("USER", "ADMIN")


                        // =============================================
                        // ADDRESSES
                        // USER + ADMIN
                        // =============================================

                        .requestMatchers(
                                "/addresses/**"
                        ).hasAnyRole("USER", "ADMIN")


                        // =============================================
                        // EVERYTHING ELSE
                        // =============================================

                        .anyRequest().authenticated()
                )


                // =============================================
                // GOOGLE OAUTH2
                // =============================================

                .oauth2Login(oauth ->
                        oauth.successHandler(
                                googleOAuth2SuccessHandler
                        )
                )


                // =============================================
                // EXCEPTION HANDLING
                // =============================================

                .exceptionHandling(exception -> exception

                        // 401 - Not authenticated
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )

                        // 403 - Authenticated but forbidden
                        .accessDeniedHandler(
                                accessDeniedHandler
                        )
                )


                // =============================================
                // JWT FILTER
                // =============================================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }
}