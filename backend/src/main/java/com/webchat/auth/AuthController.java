package com.webchat.auth;

import com.webchat.auth.dto.AccountDeleteRequest;
import com.webchat.auth.dto.LoginRequest;
import com.webchat.auth.dto.PasswordChangeRequest;
import com.webchat.auth.dto.PasswordResetConfirmRequest;
import com.webchat.auth.dto.PasswordResetRequest;
import com.webchat.auth.dto.RegisterRequest;
import com.webchat.auth.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final UserRepository users;
    private final CurrentUserResolver currentUser;

    public AuthController(AuthService auth, UserRepository users, CurrentUserResolver currentUser) {
        this.auth = auth;
        this.users = users;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        User u = auth.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(u));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest req,
                              HttpServletRequest httpReq, HttpServletResponse resp) {
        return UserResponse.from(auth.login(req, httpReq, resp));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest req, HttpServletResponse resp) {
        auth.refresh(req, resp);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse resp) {
        CurrentUser cu = currentUser.require();
        auth.logout(cu.sessionId(), resp);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> passwordResetRequest(@Valid @RequestBody PasswordResetRequest req) {
        auth.requestPasswordReset(req.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> passwordResetConfirm(@Valid @RequestBody PasswordResetConfirmRequest req) {
        auth.confirmPasswordReset(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-change")
    public ResponseEntity<Void> passwordChange(@Valid @RequestBody PasswordChangeRequest req) {
        auth.changePassword(currentUser.require().userId(), req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody AccountDeleteRequest req,
                                              HttpServletResponse resp) {
        auth.deleteAccount(currentUser.require().userId(), req.password(), resp);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me() {
        Long id = currentUser.require().userId();
        return UserResponse.from(users.findById(id).orElseThrow());
    }
}
