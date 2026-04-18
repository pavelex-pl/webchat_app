package com.webchat.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthCookies cookies;
    private final JwtService jwt;
    private final SessionRepository sessions;

    public JwtAuthFilter(AuthCookies cookies, JwtService jwt, SessionRepository sessions) {
        this.cookies = cookies;
        this.jwt = jwt;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = cookies.readAccess(request);
        if (token != null && !token.isBlank()) {
            try {
                JwtService.Parsed parsed = jwt.parse(token);
                sessions.findById(parsed.sessionId())
                        .filter(Session::isActive)
                        .ifPresent(s -> {
                            CurrentUser cu = new CurrentUser(parsed.userId(), parsed.sessionId());
                            AuthToken auth = new AuthToken(cu);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (Exception ignored) {
                // invalid / expired token — leave unauthenticated
            }
        }
        chain.doFilter(request, response);
    }

    static final class AuthToken extends AbstractAuthenticationToken {
        private final CurrentUser principal;

        AuthToken(CurrentUser principal) {
            super(List.of());
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override public Object getCredentials() { return null; }
        @Override public Object getPrincipal() { return principal; }
    }
}
