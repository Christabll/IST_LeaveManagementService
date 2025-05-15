package com.christabella.africahr.leavemanagement.security;

import com.christabella.africahr.leavemanagement.Config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/webjars")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = resolveToken(request);

            if (token != null) {
                if (!jwtTokenProvider.validateToken(token)) {
                    logger.warn("Invalid JWT token");
                    respondUnauthorized(response, "Invalid or expired token.");
                    return;
                }

                String userId = jwtTokenProvider.getUserId(token);
                String email = jwtTokenProvider.getEmail(token);
                List<String> roles = jwtTokenProvider.getRoles(token);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> role.trim().toUpperCase())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                logger.debug("User {} authenticated with roles: {}", email, authorities);

                CustomUserDetails customUserDetails = new CustomUserDetails(userId, email, null, authorities);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(customUserDetails, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            logger.error("JWT filter error: {}", ex.getMessage());
            respondUnauthorized(response, "Unauthorized: " + ex.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(jwtProperties.getPrefix())) {
            return header.substring(jwtProperties.getPrefix().length());
        }
        return null;
    }

    private void respondUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false, \"message\":\"" + message + "\"}");
    }

}
