package com.webchat.auth;

import com.webchat.auth.dto.LoginRequest;
import com.webchat.auth.dto.PasswordChangeRequest;
import com.webchat.auth.dto.PasswordResetConfirmRequest;
import com.webchat.auth.dto.RegisterRequest;
import com.webchat.common.BadRequestException;
import com.webchat.common.ConflictException;
import com.webchat.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy policy;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final AuthCookies cookies;

    public AuthService(UserRepository users, SessionRepository sessions,
                       PasswordResetTokenRepository resetTokens,
                       PasswordEncoder passwordEncoder, PasswordPolicy policy,
                       JwtService jwt, RefreshTokenService refreshTokens, AuthCookies cookies) {
        this.users = users;
        this.sessions = sessions;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.policy = policy;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.cookies = cookies;
    }

    @Transactional
    public User register(RegisterRequest req) {
        policy.validate(req.password());
        if (users.existsByEmail(req.email())) throw new ConflictException("Email already registered");
        if (users.existsByUsername(req.username())) throw new ConflictException("Username already taken");
        User u = new User(req.email(), req.username(), passwordEncoder.encode(req.password()));
        return users.save(u);
    }

    @Transactional
    public User login(LoginRequest req, HttpServletRequest httpReq, HttpServletResponse resp) {
        User u = users.findByEmail(req.email())
                .filter(x -> x.getDeletedAt() == null)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        Session session = openSession(u.getId(), httpReq, resp);
        log.info("login userId={} sessionId={}", u.getId(), session.getId());
        return u;
    }

    @Transactional
    public Session refresh(HttpServletRequest req, HttpServletResponse resp) {
        String token = cookies.readRefresh(req);
        if (token == null) throw new UnauthorizedException("No refresh token");
        String hash = refreshTokens.hash(token);
        Session session = sessions.findByRefreshHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (!session.isActive()) throw new UnauthorizedException("Session expired");
        String newToken = refreshTokens.generate();
        session.setRefreshHash(refreshTokens.hash(newToken));
        session.setLastSeenAt(Instant.now());
        sessions.save(session);
        String access = jwt.issueAccessToken(session.getUserId(), session.getId());
        cookies.setAccessCookie(resp, access, jwt.accessTtl());
        cookies.setRefreshCookie(resp, newToken, RefreshTokenService.TTL);
        return session;
    }

    @Transactional
    public void logout(UUID sessionId, HttpServletResponse resp) {
        sessions.findById(sessionId).ifPresent(s -> {
            s.setRevokedAt(Instant.now());
            sessions.save(s);
        });
        cookies.clearAccessCookie(resp);
        cookies.clearRefreshCookie(resp);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest req) {
        User u = users.findById(userId).orElseThrow(() -> new UnauthorizedException("Not found"));
        if (!passwordEncoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new UnauthorizedException("Current password incorrect");
        }
        policy.validate(req.newPassword());
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        users.save(u);
    }

    @Transactional
    public String requestPasswordReset(String email) {
        Optional<User> maybe = users.findByEmail(email).filter(u -> u.getDeletedAt() == null);
        if (maybe.isEmpty()) {
            log.info("password reset requested for unknown email={} (ignored)", email);
            return null;
        }
        User u = maybe.get();
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = refreshTokens.hash(token);
        resetTokens.save(new PasswordResetToken(hash, u.getId(), Instant.now().plus(RESET_TOKEN_TTL)));
        log.info("PASSWORD RESET LINK (demo only) userId={} token={}", u.getId(), token);
        return token;
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest req) {
        policy.validate(req.newPassword());
        String hash = refreshTokens.hash(req.token());
        PasswordResetToken prt = resetTokens.findById(hash)
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));
        User u = users.findById(prt.getUserId()).orElseThrow(() -> new BadRequestException("User missing"));
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        users.save(u);
        prt.setUsedAt(Instant.now());
        resetTokens.save(prt);
        sessions.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(u.getId())
                .forEach(s -> { s.setRevokedAt(Instant.now()); sessions.save(s); });
    }

    @Transactional
    public void deleteAccount(Long userId, String password, HttpServletResponse resp) {
        User u = users.findById(userId).orElseThrow(() -> new UnauthorizedException("Not found"));
        if (!passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new UnauthorizedException("Password incorrect");
        }
        users.delete(u);
        cookies.clearAccessCookie(resp);
        cookies.clearRefreshCookie(resp);
    }

    public Session openSession(Long userId, HttpServletRequest req, HttpServletResponse resp) {
        String refresh = refreshTokens.generate();
        UUID sessionId = UUID.randomUUID();
        Session session = new Session(
                sessionId, userId, refreshTokens.hash(refresh),
                truncate(req.getHeader("User-Agent"), 512), clientIp(req),
                Instant.now().plus(RefreshTokenService.TTL)
        );
        sessions.save(session);
        String access = jwt.issueAccessToken(userId, sessionId);
        cookies.setAccessCookie(resp, access, jwt.accessTtl());
        cookies.setRefreshCookie(resp, refresh, RefreshTokenService.TTL);
        return session;
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
