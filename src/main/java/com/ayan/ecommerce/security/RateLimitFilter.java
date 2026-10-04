package com.ayan.ecommerce.security;

import com.ayan.ecommerce.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    // Global API limit
    private static final int MAX_REQUESTS = 60;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    // Endpoint-specific limits
    private final Map<String, RateLimitPolicy> endpointPolicies = Map.of(

            "POST /auth/login",
            new RateLimitPolicy(5, Duration.ofMinutes(10)),

            "POST /auth/register",
            new RateLimitPolicy(5, Duration.ofMinutes(10)),

            "POST /auth/verify",
            new RateLimitPolicy(5, Duration.ofMinutes(10)),

            "POST /auth/forget-password",
            new RateLimitPolicy(3, Duration.ofMinutes(15)),

            "POST /auth/reset-password",
            new RateLimitPolicy(5, Duration.ofMinutes(10)),

            "POST /auth/resend-verification-otp",
            new RateLimitPolicy(3, Duration.ofMinutes(10)),

            "POST /auth/resend-password-reset-otp",
            new RateLimitPolicy(3, Duration.ofMinutes(10)),

            "POST /auth/verify-login-otp",
            new RateLimitPolicy(5,Duration.ofMinutes(10))

    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ipAddress = getClientIP(request);

        /*
         * =========================================================
         * 1. GLOBAL RATE LIMIT
         * =========================================================
         *
         * Every request from this IP shares the same
         * 60 requests / 1 minute counter.
         */
        String globalKey =
                "rate-limit:global:" + ipAddress;

        boolean globalAllowed =
                rateLimitService.isAllowed(
                        globalKey,
                        MAX_REQUESTS,
                        WINDOW
                );

        if (!globalAllowed) {

            response.setStatus(
                    HttpStatus.TOO_MANY_REQUESTS.value()
            );

            response.setContentType(
                    "application/json"
            );

            response.getWriter().write(
                    """
                    {
                        "status": 429,
                        "message": "Too many requests. Please try again later."
                    }
                    """
            );

            return;
        }

        /*
         * =========================================================
         * 2. ENDPOINT-SPECIFIC RATE LIMIT
         * =========================================================
         *
         * Only configured sensitive endpoints get
         * an additional stricter limit.
         */
        RateLimitPolicy endpointPolicy =
                getEndpointPolicy(request);

        if (endpointPolicy != null) {

            String endpointKey =
                    "rate-limit:endpoint:"
                            + request.getMethod()
                            + ":"
                            + request.getRequestURI()
                            + ":"
                            + ipAddress;

            boolean endpointAllowed =
                    rateLimitService.isAllowed(
                            endpointKey,
                            endpointPolicy.getMaxRequests(),
                            endpointPolicy.getWindow()
                    );

            if (!endpointAllowed) {

                response.setStatus(
                        HttpStatus.TOO_MANY_REQUESTS.value()
                );

                response.setContentType(
                        "application/json"
                );

                response.getWriter().write(
                        """
                        {
                            "status": 429,
                            "message": "Too many requests for this endpoint. Please try again later."
                        }
                        """
                );

                return;
            }
        }

        /*
         * =========================================================
         * 3. CONTINUE REQUEST
         * =========================================================
         */
        filterChain.doFilter(request, response);
    }

    /**
     * Finds the client's IP address.
     */
    private String getClientIP(HttpServletRequest request) {

        String forwardedFor =
                request.getHeader("X-Forwarded-For");

        if (forwardedFor != null &&
                !forwardedFor.isBlank()) {

            return forwardedFor
                    .split(",")[0]
                    .trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * Finds an endpoint-specific rate limit policy.
     */
    private RateLimitPolicy getEndpointPolicy(
            HttpServletRequest request
    ) {

        String endpoint =
                request.getMethod()
                        + " "
                        + request.getRequestURI();

        return endpointPolicies.get(endpoint);
    }
}