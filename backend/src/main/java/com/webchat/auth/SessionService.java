package com.webchat.auth;

import com.webchat.auth.dto.SessionResponse;
import com.webchat.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final SessionRepository sessions;

    public SessionService(SessionRepository sessions) {
        this.sessions = sessions;
    }

    public List<SessionResponse> list(Long userId, UUID currentSessionId) {
        return sessions.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId).stream()
                .map(s -> SessionResponse.from(s, s.getId().equals(currentSessionId)))
                .toList();
    }

    @Transactional
    public void revoke(Long userId, UUID sessionId) {
        Session s = sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (s.getRevokedAt() == null) {
            s.setRevokedAt(Instant.now());
            sessions.save(s);
        }
    }
}
