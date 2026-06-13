package com.ayan.ecommerce.security;

import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.repository.UserRepository;
import com.ayan.ecommerce.service.AuthService;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtService jwtService;
    private final UserRepository repository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException, java.io.IOException {
        System.out.println("JWT FILTER HIT");
        final String authHeader =
                request.getHeader("Authorization");

        if(authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request,response);
            return;
        }

        String jwt =
                authHeader.substring(7);

        String userEmail =
                jwtService.extractUsername(jwt);

        User user = repository
                .findByEmail(userEmail)
                .orElseThrow();

        System.out.println("User Role = " + user.getRole());

        System.out.println(
                "Token Validation = "
                        + jwtService.validToken(jwt,user)
        );

        System.out.println("Authenticated Role = " + user.getRole());

        if(jwtService.validToken(jwt,user)){

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + user.getRole().name()
                    );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authToken);
        }

        filterChain.doFilter(request,response);
    }


}
