package com.colorcraze.configs.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    private record UserRateKey(String uid, String endpoint) {}

    private static class RateInfo {
        long timestamp = Instant.now().getEpochSecond();
        int count = 0;
    }

    private final Map<UserRateKey, RateInfo> rateMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RateLimit annotation = method.getMethodAnnotation(RateLimit.class);
        if (annotation == null) {
            return true;
        }

        Integer limit = annotation.limit();

        String uid = (String) request.getAttribute("firebaseUid");
        if (uid == null) {
            return true;
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

            if (info.count >= limit) {
                response.setStatus(429);
                response.getWriter().write("Too Many Requests (Rate Limit Exceeded)");
                return false;
            }

            info.count++;
        }

        return true;
    }
}
