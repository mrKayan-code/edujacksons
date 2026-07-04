package dev.edujacksons.auth.api;

import dev.edujacksons.auth.api.dto.AuthResponse;
import dev.edujacksons.auth.api.dto.LoginRequest;
import dev.edujacksons.auth.api.dto.RegisterRequest;
import dev.edujacksons.auth.api.dto.UserResponse;
import dev.edujacksons.auth.domain.User;
import dev.edujacksons.auth.service.AuthResult;
import dev.edujacksons.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(
                request.email(), request.password(), request.displayName(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.email(), request.password());
        return ResponseEntity.ok(toResponse(result));
    }

    /** Текущий пользователь. Principal — id из JWT (см. {@code JwtAuthenticationFilter}). */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UUID userId) {
        User user = authService.getById(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    private AuthResponse toResponse(AuthResult result) {
        return AuthResponse.bearer(
                result.token(), result.expiresInSeconds(), UserResponse.from(result.user()));
    }
}
