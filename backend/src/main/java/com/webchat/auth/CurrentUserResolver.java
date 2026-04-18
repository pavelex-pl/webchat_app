package com.webchat.auth;

import com.webchat.common.UnauthorizedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    public CurrentUser require() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CurrentUser cu)) {
            throw new UnauthorizedException("Authentication required");
        }
        return cu;
    }
}
