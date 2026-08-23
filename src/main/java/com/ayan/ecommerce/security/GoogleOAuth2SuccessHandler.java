
        package com.ayan.ecommerce.security;

import com.ayan.ecommerce.dto.ApiResponse;
import com.ayan.ecommerce.entity.Cart;
import com.ayan.ecommerce.entity.Role;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.repository.CartRepository;
import com.ayan.ecommerce.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // ==========================================
        // GOOGLE USER
        // ==========================================

        OAuth2User oauth2User =
                (OAuth2User) authentication.getPrincipal();

        String email =
                oauth2User.getAttribute("email");

        String name =
                oauth2User.getAttribute("name");


        // ==========================================
        // CHECK USER
        // ==========================================

        User user =
                userRepository.findByEmail(email)
                        .orElse(null);


        // ==========================================
        // NEW GOOGLE USER
        // ==========================================

        if (user == null) {

            user = User.builder()
                    .name(name != null ? name : "Google User")
                    .email(email)

                    // Google user ke liye local password
                    // login mein use nahi hoga
                    .password(
                            passwordEncoder.encode(
                                    UUID.randomUUID().toString()
                            )
                    )

                    .role(Role.USER)

                    // Google successfully authenticated
                    .verified(true)

                    // Google user ke paas initially
                    // phone number nahi hai
                    .phoneNumber(null)

                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            user = userRepository.save(user);


            // ==========================================
            // CREATE CART
            // ==========================================

            Cart cart = Cart.builder()
                    .user(user)
                    .build();

            cartRepository.save(cart);
        }


        // ==========================================
        // EXISTING USER
        // ==========================================

        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
        }


        // ==========================================
        // GENERATE OUR JWT
        // ==========================================

        String jwt =
                jwtService.generateToken(user);


        // ==========================================
        // API RESPONSE
        // ==========================================

        ApiResponse<String> apiResponse =
                new ApiResponse<>(
                        true,
                        "Google login successful",
                        jwt,
                        LocalDateTime.now()
                );


        // ==========================================
        // SEND JSON RESPONSE
        // ==========================================

        response.setStatus(
                HttpServletResponse.SC_OK
        );

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        objectMapper.writeValue(
                response.getWriter(),
                apiResponse
        );
    }
}
