package com.colorcraze.configs.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.colorcraze.configs.ratelimit.RateLimitResolver;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitResolver resolver;

    public RateLimitFilter(RateLimitResolver resolver) {
        this.resolver = resolver;
    }

    private record UserRateKey(String uid, String endpoint) {}

    private static class RateInfo {
        long timestamp = Instant.now().getEpochSecond();
        int count = 0;
    }

    private final Map<UserRateKey, RateInfo> rateMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uid = (String) request.getAttribute("firebaseUid");
        System.out.print(uid);
        if (uid == null) { 
            filterChain.doFilter(request, response);
            return;
        }

        Integer customLimit = resolver.resolveLimit(request);
        
        if (customLimit == null) {
            System.out.print(customLimit);
            filterChain.doFilter(request, response);
            return;
        }

        String endpoint = request.getMethod() + ":" + request.getRequestURI();
        UserRateKey key = new UserRateKey(uid, endpoint);

        RateInfo info = rateMap.computeIfAbsent(key, k -> new RateInfo());
        long now = Instant.now().getEpochSecond();

        synchronized (info) {
            if (now - info.timestamp >= 60) {
                info.timestamp = now;
                info.count = 0;
            }

            if (info.count >= customLimit) {
                response.setStatus(429);
                response.getWriter().write("Too Many Requests (Rate Limit Exceeded)");
                return;
            }

            info.count++;
        }

        filterChain.doFilter(request, response);
    }
}
