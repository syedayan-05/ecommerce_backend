package com.ayan.ecommerce.security;
import java.time.Duration;

public class RateLimitPolicy {

    private final int maxRequests;
    private final Duration window;

    public RateLimitPolicy(
            int maxRequests,
            Duration window
    ) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public Duration getWindow() {
        return window;
    }
}