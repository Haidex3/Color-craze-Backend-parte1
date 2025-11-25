package com.colorcraze.configs.filters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that validates Firebase ID tokens for incoming requests.
 * Sets the Spring Security context if the token is valid.
 */
@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    /**
     * Filters incoming HTTP requests and verifies Firebase ID tokens in the Authorization header.
     * If the token is valid, the user's authentication is set in the SecurityContext.
     * Invalid or missing tokens result in a 401 Unauthorized response.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param chain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);

            String uid = decoded.getUid();

            request.setAttribute("firebaseUid", uid);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            uid,
                            null,
                            List.of()
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase ID Token");
            return;
        }

        chain.doFilter(request, response);
    }
}
