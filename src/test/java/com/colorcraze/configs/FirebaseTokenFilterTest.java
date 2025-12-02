package com.colorcraze.configs;

import com.colorcraze.configs.filters.FirebaseTokenFilter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class FirebaseTokenFilterTest {

    private FirebaseTokenFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private MockedStatic<FirebaseAuth> firebaseAuthMock;
    private FirebaseAuth firebaseAuth;

    @BeforeEach
    void setUp() {
        filter = new FirebaseTokenFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();

        firebaseAuth = mock(FirebaseAuth.class);
        firebaseAuthMock = mockStatic(FirebaseAuth.class);
        firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
    }

    @AfterEach
    void tearDown() {
        firebaseAuthMock.close();
    }

    @Test
    void shouldAllowOptionsRequest() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipAuthForPublicPaths() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueWhenNoAuthorizationHeader() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/secure/data");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAuthenticateWhenValidToken() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/secure");
        when(request.getHeader("Authorization")).thenReturn("Bearer validToken");

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("user123");
        when(firebaseAuth.verifyIdToken("validToken")).thenReturn(firebaseToken);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("user123");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldReturn401OnInvalidToken() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/secure");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");

        firebaseAuthMock.when(() -> firebaseAuth.verifyIdToken("invalid"))
                .thenThrow(new RuntimeException("Token inválido"));

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase ID Token");
        verify(chain, never()).doFilter(any(), any());
    }
}
