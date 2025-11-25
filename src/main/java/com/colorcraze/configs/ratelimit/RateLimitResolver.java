package com.colorcraze.configs.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RateLimitResolver {

    public Integer resolveLimit(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

        if (handler instanceof HandlerMethod handlerMethod) {
            RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (annotation != null) {
                return annotation.limit();
            }
        }

        return null;
    }
}

