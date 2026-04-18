package com.webchat.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthCookies {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    public static final String REFRESH_PATH = "/api/auth/refresh";

    private final boolean secure;

    public AuthCookies(@Value("${webchat.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public void setAccessCookie(HttpServletResponse resp, String value, Duration ttl) {
        append(resp, ACCESS_COOKIE, value, "/", ttl);
    }

    public void setRefreshCookie(HttpServletResponse resp, String value, Duration ttl) {
        append(resp, REFRESH_COOKIE, value, REFRESH_PATH, ttl);
    }

    public void clearAccessCookie(HttpServletResponse resp) {
        append(resp, ACCESS_COOKIE, "", "/", Duration.ZERO);
    }

    public void clearRefreshCookie(HttpServletResponse resp) {
        append(resp, REFRESH_COOKIE, "", REFRESH_PATH, Duration.ZERO);
    }

    public String readAccess(HttpServletRequest req) { return read(req, ACCESS_COOKIE); }
    public String readRefresh(HttpServletRequest req) { return read(req, REFRESH_COOKIE); }

    private String read(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void append(HttpServletResponse resp, String name, String value, String path, Duration maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append('=').append(value);
        sb.append("; Path=").append(path);
        sb.append("; HttpOnly");
        sb.append("; SameSite=Lax");
        if (secure) sb.append("; Secure");
        sb.append("; Max-Age=").append(maxAge.getSeconds());
        resp.addHeader("Set-Cookie", sb.toString());
    }
}
