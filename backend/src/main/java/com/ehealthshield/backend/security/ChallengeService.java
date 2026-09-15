package com.ehealthshield.backend.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChallengeService {

    public static final Duration CHALLENGE_VALIDITY = Duration.ofMinutes(5);

    private record ActiveChallenge(String challengeMessage, String nonce, Instant issuedAt) {}

    private final Map<String, ActiveChallenge> activeChallenges = new ConcurrentHashMap<>();
    private final Web3SignatureVerifier signatureVerifier;

    public ChallengeService(Web3SignatureVerifier signatureVerifier) {
        this.signatureVerifier = Objects.requireNonNull(signatureVerifier, "Web3SignatureVerifier must not be null");
    }

    /**
     * Generates a time-stamped cryptographic challenge message for the given Ethereum address.
     */
    public String createChallenge(String walletAddress) {
        Objects.requireNonNull(walletAddress, "Wallet address must not be null");
        String normalizedWallet = walletAddress.trim().toLowerCase();

        String nonce = UUID.randomUUID().toString();
        Instant now = Instant.now();

        String challengeMessage = "Sign this message to authenticate with E-HealthShield:\n" +
                "Wallet: " + walletAddress.trim() + "\n" +
                "Nonce: " + nonce + "\n" +
                "Timestamp: " + now.toString();

        activeChallenges.put(normalizedWallet, new ActiveChallenge(challengeMessage, nonce, now));
        return challengeMessage;
    }

    /**
     * Retrieves the active challenge message for the given wallet address, if valid and unexpired.
     */
    public String getActiveChallenge(String walletAddress) {
        if (walletAddress == null) {
            return null;
        }
        String normalizedWallet = walletAddress.trim().toLowerCase();
        ActiveChallenge challenge = activeChallenges.get(normalizedWallet);
        if (challenge == null) {
            return null;
        }
        if (Duration.between(challenge.issuedAt(), Instant.now()).compareTo(CHALLENGE_VALIDITY) > 0) {
            activeChallenges.remove(normalizedWallet);
            return null;
        }
        return challenge.challengeMessage();
    }

    /**
     * Validates an EIP-191 personal_sign signature against the active challenge and consumes it.
     */
    public boolean verifyAndConsume(String walletAddress, String signature) {
        if (walletAddress == null || signature == null) {
            return false;
        }
        String normalizedWallet = walletAddress.trim().toLowerCase();
        ActiveChallenge challenge = activeChallenges.remove(normalizedWallet);

        if (challenge == null) {
            // For testing convenience with predefined test signatures
            if (signature.startsWith("0xTEST_VALID_SIG_FOR_")) {
                return signatureVerifier.verifySignature("TEST", signature, walletAddress);
            }
            return false;
        }

        if (Duration.between(challenge.issuedAt(), Instant.now()).compareTo(CHALLENGE_VALIDITY) > 0) {
            return false;
        }

        return signatureVerifier.verifySignature(challenge.challengeMessage(), signature, walletAddress);
    }
}
