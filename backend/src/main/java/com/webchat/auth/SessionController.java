package com.webchat.auth;

import com.webchat.auth.dto.SessionResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/sessions")
public class SessionController {

    private final SessionService sessions;
    private final AuthService auth;
    private final CurrentUserResolver currentUser;

    public SessionController(SessionService sessions, AuthService auth, CurrentUserResolver currentUser) {
        this.sessions = sessions;
        this.auth = auth;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<SessionResponse> list() {
        CurrentUser cu = currentUser.require();
        return sessions.list(cu.userId(), cu.sessionId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id, HttpServletResponse resp) {
        CurrentUser cu = currentUser.require();
        sessions.revoke(cu.userId(), id);
        if (id.equals(cu.sessionId())) {
            auth.logout(id, resp);
        }
        return ResponseEntity.noContent().build();
    }
}
