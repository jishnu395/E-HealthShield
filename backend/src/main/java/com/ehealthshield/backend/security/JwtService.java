package com.ehealthshield.backend.security;

import com.ehealthshield.backend.entity.UserRole;
import com.ehealthshield.backend.exception.CryptographicException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);

    @Value("${app.jwt.secret:default_super_secret_jwt_key_for_ehealthshield_auth_256_bits_minimum_123456}")
    private String jwtSecret;

    private SecretKeySpec secretKeySpec;

    public JwtService() {
    }

    public JwtService(String secret) {
        this.jwtSecret = secret;
        init();
    }

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            jwtSecret = "default_super_secret_jwt_key_for_ehealthshield_auth_256_bits_minimum_123456";
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.secretKeySpec = new SecretKeySpec(keyBytes, HMAC_SHA256);
    }

    /**
     * Generates a compact HMAC-SHA256 signed JWT token containing wallet address and user role.
     */
    public String generateToken(String walletAddress, UserRole role) {
        Objects.requireNonNull(walletAddress, "Wallet address must not be null");
        Objects.requireNonNull(role, "User role must not be null");

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        long now = Instant.now().getEpochSecond();
        long exp = Instant.now().plus(TOKEN_VALIDITY).getEpochSecond();

        String payloadJson = "{\"sub\":\"" + walletAddress.trim().toLowerCase() + "\",\"role\":\"" + role.name() + "\",\"iat\":" + now + ",\"exp\":" + exp + "}";

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = computeHmacBase64Url(signingInput);

        return signingInput + "." + signature;
    }

    /**
     * Validates signature and expiration of the JWT token.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = computeHmacBase64Url(signingInput);

        if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = extractLongClaim(payloadJson, "exp");
            return exp > Instant.now().getEpochSecond();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the subject (wallet address) from the JWT token.
     */
    public String getWalletAddressFromToken(String token) {
        if (!validateToken(token)) {
            return null;
        }
        String[] parts = token.split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return extractStringClaim(payloadJson, "sub");
    }

    /**
     * Extracts the user role from the JWT token.
     */
    public UserRole getRoleFromToken(String token) {
        if (!validateToken(token)) {
            return null;
        }
        String[] parts = token.split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String roleStr = extractStringClaim(payloadJson, "role");
        if (roleStr == null) {
            return null;
        }
        try {
            return UserRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String computeHmacBase64Url(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptographicException("Failed to compute JWT HMAC signature", e);
        }
    }

    private String extractStringClaim(String json, String claimName) {
        Pattern pattern = Pattern.compile("\"" + claimName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private long extractLongClaim(String json, String claimName) {
        Pattern pattern = Pattern.compile("\"" + claimName + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0L;
    }
}
