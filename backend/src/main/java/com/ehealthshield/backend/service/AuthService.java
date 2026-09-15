package com.ehealthshield.backend.service;

import com.ehealthshield.backend.crypto.MlKemCryptoService;
import com.ehealthshield.backend.dto.request.AuthChallengeRequest;
import com.ehealthshield.backend.dto.request.AuthVerifyRequest;
import com.ehealthshield.backend.dto.request.UserRegisterRequest;
import com.ehealthshield.backend.dto.response.AuthChallengeResponse;
import com.ehealthshield.backend.dto.response.AuthResponse;
import com.ehealthshield.backend.entity.UserEntity;
import com.ehealthshield.backend.entity.UserRole;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.UserRepository;
import com.ehealthshield.backend.security.ChallengeService;
import com.ehealthshield.backend.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern ETHEREUM_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    private final UserRepository userRepository;
    private final ChallengeService challengeService;
    private final JwtService jwtService;
    private final MlKemCryptoService mlKemCryptoService;

    public AuthService(UserRepository userRepository,
                       ChallengeService challengeService,
                       JwtService jwtService,
                       MlKemCryptoService mlKemCryptoService) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository must not be null");
        this.challengeService = Objects.requireNonNull(challengeService, "ChallengeService must not be null");
        this.jwtService = Objects.requireNonNull(jwtService, "JwtService must not be null");
        this.mlKemCryptoService = Objects.requireNonNull(mlKemCryptoService, "MlKemCryptoService must not be null");
    }

    /**
     * Issues an ephemeral cryptographic authentication challenge for a wallet address.
     */
    public AuthChallengeResponse requestChallenge(AuthChallengeRequest request) {
        if (request == null || request.walletAddress() == null || request.walletAddress().isBlank()) {
            throw new ValidationException("Wallet address is required");
        }

        String wallet = request.walletAddress().trim();
        validateWalletAddress(wallet);

        String challengeMessage = challengeService.createChallenge(wallet);
        return new AuthChallengeResponse(wallet, challengeMessage);
    }

    /**
     * Verifies the EIP-191 signature against the active challenge and issues a JWT token.
     */
    @Transactional
    public AuthResponse verifyAndAuthenticate(AuthVerifyRequest request) {
        if (request == null) {
            throw new ValidationException("Verification request must not be null");
        }
        if (request.walletAddress() == null || request.walletAddress().isBlank()) {
            throw new ValidationException("Wallet address is required");
        }
        if (request.signature() == null || request.signature().isBlank()) {
            throw new ValidationException("Cryptographic signature is required");
        }

        String wallet = request.walletAddress().trim();
        validateWalletAddress(wallet);

        boolean isValid = challengeService.verifyAndConsume(wallet, request.signature().trim());
        if (!isValid) {
            throw new ValidationException("Invalid Web3 cryptographic signature or expired challenge");
        }

        // Look up registered user or create a default user record if not present
        UserEntity user = userRepository.findByWalletAddressIgnoreCase(wallet).orElseGet(() -> {
            byte[] defaultKyberKey = mlKemCryptoService.generateKeyPair().publicKey();
            UserEntity newUser = UserEntity.builder()
                    .walletAddress(wallet)
                    .role(UserRole.PATIENT)
                    .kyberPublicKey(defaultKyberKey)
                    .createdAt(Instant.now())
                    .build();
            return userRepository.save(newUser);
        });

        String token = jwtService.generateToken(user.getWalletAddress(), user.getRole());
        return new AuthResponse(token, user.getWalletAddress(), user.getRole(), 86400L);
    }

    /**
     * Explicitly registers a user with their role and ML-KEM-768 public key.
     */
    @Transactional
    public AuthResponse registerUser(UserRegisterRequest request) {
        if (request == null) {
            throw new ValidationException("Registration request must not be null");
        }
        if (request.walletAddress() == null || request.walletAddress().isBlank()) {
            throw new ValidationException("Wallet address is required");
        }
        if (request.role() == null) {
            throw new ValidationException("User role is required (PATIENT or DOCTOR)");
        }

        String wallet = request.walletAddress().trim();
        validateWalletAddress(wallet);

        byte[] kyberPubKey;
        if (request.kyberPublicKeyBase64() != null && !request.kyberPublicKeyBase64().isBlank()) {
            try {
                kyberPubKey = Base64.getDecoder().decode(request.kyberPublicKeyBase64().trim());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid Base64 format for ML-KEM public key");
            }
            if (kyberPubKey.length != MlKemCryptoService.PUBLIC_KEY_SIZE_BYTES) {
                throw new ValidationException("Invalid ML-KEM-768 public key size: expected "
                        + MlKemCryptoService.PUBLIC_KEY_SIZE_BYTES + " bytes, received " + kyberPubKey.length);
            }
        } else {
            kyberPubKey = mlKemCryptoService.generateKeyPair().publicKey();
        }

        UserEntity user = userRepository.findByWalletAddressIgnoreCase(wallet).orElseGet(() ->
                UserEntity.builder()
                        .walletAddress(wallet)
                        .createdAt(Instant.now())
                        .build()
        );

        user.setRole(request.role());
        if (kyberPubKey != null) {
            user.setKyberPublicKey(kyberPubKey);
        }

        UserEntity savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getWalletAddress(), savedUser.getRole());
        return new AuthResponse(token, savedUser.getWalletAddress(), savedUser.getRole(), 86400L);
    }

    /**
     * Retrieves the profile of an authenticated user.
     */
    @Transactional(readOnly = true)
    public UserEntity getUserByWallet(String walletAddress) {
        if (walletAddress == null || walletAddress.isBlank()) {
            throw new ValidationException("Wallet address is required");
        }
        return userRepository.findByWalletAddressIgnoreCase(walletAddress.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for wallet: " + walletAddress));
    }

    private void validateWalletAddress(String wallet) {
        if (!ETHEREUM_ADDRESS_PATTERN.matcher(wallet).matches()) {
            throw new ValidationException("Invalid Ethereum wallet address format (must be 0x followed by 40 hex chars)");
        }
    }
}
