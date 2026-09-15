package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.dto.request.AuthChallengeRequest;
import com.ehealthshield.backend.dto.request.AuthVerifyRequest;
import com.ehealthshield.backend.dto.request.UserRegisterRequest;
import com.ehealthshield.backend.dto.response.AuthChallengeResponse;
import com.ehealthshield.backend.dto.response.AuthResponse;
import com.ehealthshield.backend.entity.UserEntity;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.security.UserPrincipal;
import com.ehealthshield.backend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = Objects.requireNonNull(authService, "AuthService must not be null");
    }

    /**
     * Step 1 of Web3 authentication: Request an authentication challenge.
     */
    @PostMapping("/challenge")
    public ResponseEntity<AuthChallengeResponse> requestChallenge(@RequestBody AuthChallengeRequest request) {
        AuthChallengeResponse response = authService.requestChallenge(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2 of Web3 authentication: Verify MetaMask signature and issue JWT token.
     */
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifySignature(@RequestBody AuthVerifyRequest request) {
        AuthResponse response = authService.verifyAndAuthenticate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * User registration with ML-KEM-768 public key.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody UserRegisterRequest request) {
        AuthResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get profile of the currently authenticated Web3 user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserEntity> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ValidationException("No authenticated Web3 user in security context");
        }
        UserEntity user = authService.getUserByWallet(principal.getWalletAddress());
        return ResponseEntity.ok(user);
    }
}
