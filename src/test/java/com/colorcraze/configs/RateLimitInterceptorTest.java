package com.colorcraze.configs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import com.colorcraze.configs.ratelimit.RateLimit;
import com.colorcraze.configs.ratelimit.RateLimitConfig;
import com.colorcraze.configs.ratelimit.RateLimitInterceptor;
import com.colorcraze.configs.ratelimit.RateLimitResolver;

import java.io.PrintWriter;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {
        interceptor = new RateLimitInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = mock(PrintWriter.class);

        when(response.getWriter()).thenReturn(writer);
    }

    @RateLimit(limit = 1)
    public void testEndpoint() {
        // Método intencionalmente vacío: únicamente para tener un HandlerMethod anotado con @RateLimit en los tests.
    }

    private HandlerMethod getHandlerMethod() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("testEndpoint");
        return new HandlerMethod(this, method);
    }

    @Test
    void preHandle_AllowsFirstRequest() throws Exception {
        when(request.getAttribute("firebaseUid")).thenReturn("user1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        boolean result = interceptor.preHandle(request, response, getHandlerMethod());

        assertTrue(result);
        verify(response, never()).setStatus(429);
    }

    @Test
    void preHandle_BlocksAfterLimitExceeded() throws Exception {
        when(request.getAttribute("firebaseUid")).thenReturn("user1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        HandlerMethod handlerMethod = getHandlerMethod();

        assertTrue(interceptor.preHandle(request, response, handlerMethod));

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(429);
        verify(writer).write("Too Many Requests (Rate Limit Exceeded)");
    }

    @Test
    void preHandle_NoAnnotation_ReturnsTrue() throws Exception {
        Object handler = new Object();
        assertTrue(interceptor.preHandle(request, response, handler));
    }

    @Test
    void preHandle_NoUid_ReturnsTrue() throws Exception {
        when(request.getAttribute("firebaseUid")).thenReturn(null);
        assertTrue(interceptor.preHandle(request, response, getHandlerMethod()));
    }

    @Test
    void resolveLimit_ReturnsLimit_WhenAnnotationPresent() throws Exception {
        RateLimitResolver resolver = new RateLimitResolver();
        HttpServletRequest request4 = mock(HttpServletRequest.class);

        HandlerMethod handlerMethod = getHandlerMethod();

        when(request4.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
                .thenReturn(handlerMethod);
        Integer limit = resolver.resolveLimit(request4);

        assertNotNull(limit);
        assertEquals(1, limit);
    }

    @Test
    void resolveLimit_ReturnsNull_WhenNoAnnotationPresent() throws Exception {
        RateLimitResolver resolver = new RateLimitResolver();
        HttpServletRequest request3 = mock(HttpServletRequest.class);

        Method noAnnotationMethod = this.getClass().getMethod("methodWithoutRateLimit");
        HandlerMethod handlerMethod = new HandlerMethod(this, noAnnotationMethod);

        when(request3.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
                .thenReturn(handlerMethod);
        Integer limit = resolver.resolveLimit(request3);

        assertNull(limit);
    }

    @Test
    void resolveLimit_ReturnsNull_WhenHandlerIsNotHandlerMethod() {
        RateLimitResolver resolver = new RateLimitResolver();
        HttpServletRequest request2 = mock(HttpServletRequest.class);

        when(request2.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
                .thenReturn(new Object());

        Integer limit = resolver.resolveLimit(request2);
        assertNull(limit);
    }

    @Test
    void addInterceptors_ShouldRegisterRateLimitInterceptor() {
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        RateLimitConfig config = new RateLimitConfig();

        config.addInterceptors(registry);
        verify(registry, times(1)).addInterceptor(any(RateLimitInterceptor.class));
    }

    public void methodWithoutRateLimit() {
        // sin anotación, usado para testing
    }
}
