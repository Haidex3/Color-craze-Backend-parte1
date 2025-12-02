package com.colorcraze.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.colorcraze.configs.filters.LoggingContextFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoggingContextFilterTest {

    private TestableLoggingContextFilter filter;
    private FilterChain chain;

    static class TestableLoggingContextFilter extends LoggingContextFilter {
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
            return super.shouldNotFilter(request);
        }
    }

    @BeforeEach
    void setup() {
        filter = new TestableLoggingContextFilter();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void shouldGenerateCorrelationIdIfMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        String headerId = response.getHeader("X-Correlation-ID");
        assertThat(headerId).isNotBlank();
    }

    @Test
    void shouldUseExistingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.addHeader("X-Correlation-ID", "abc123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("abc123");
    }

    @Test
    void shouldClearMDCAfterFilterExecution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void shouldSkipFilteringForWebSocketPaths() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRequestURI()).thenReturn("/ws/something");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        when(request.getRequestURI()).thenReturn("/color-craze/ws/game");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        when(request.getRequestURI()).thenReturn("/api/ws/info");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        when(request.getRequestURI()).thenReturn("/app/websocket");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldFilterNormalPaths() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRequestURI()).thenReturn("/api/users");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }
}
