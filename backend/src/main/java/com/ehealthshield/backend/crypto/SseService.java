package com.ehealthshield.backend.crypto;

import com.ehealthshield.backend.exception.CryptographicException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SseService {

    public static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.crypto.sse-master-key:}")
    private String sseMasterKeyProperty;

    private SecretKeySpec masterKeySpec;

    public SseService() {
    }

    public SseService(String sseMasterKey) {
        setMasterKey(sseMasterKey);
    }

    public SseService(byte[] sseMasterKeyBytes) {
        setMasterKey(sseMasterKeyBytes);
    }

    @PostConstruct
    public void init() {
        if (this.masterKeySpec == null && sseMasterKeyProperty != null && !sseMasterKeyProperty.isBlank() && !sseMasterKeyProperty.startsWith("${")) {
            setMasterKey(sseMasterKeyProperty);
        }
    }

    public void setMasterKey(String masterKey) {
        Objects.requireNonNull(masterKey, "SSE master key must not be null");
        if (masterKey.isBlank()) {
            throw new CryptographicException("SSE master key must not be empty or blank");
        }
        setMasterKey(masterKey.getBytes(StandardCharsets.UTF_8));
    }

    public void setMasterKey(byte[] masterKeyBytes) {
        Objects.requireNonNull(masterKeyBytes, "SSE master key bytes must not be null");
        if (masterKeyBytes.length == 0) {
            throw new CryptographicException("SSE master key bytes must not be empty");
        }
        this.masterKeySpec = new SecretKeySpec(masterKeyBytes, HMAC_ALGORITHM);
    }

    /**
     * Normalizes a keyword consistently by trimming whitespace and converting to lowercase.
     */
    public String normalize(String keyword) {
        if (keyword == null) {
            return "";
        }
        return keyword.trim().toLowerCase();
    }

    /**
     * Generates a deterministic HMAC-SHA256 search tag for an extracted keyword.
     */
    public String generateSearchTag(String keyword) {
        return computeHmac(normalize(keyword), getSecretKeySpec());
    }

    /**
     * Generates a deterministic search trapdoor for a query keyword (identical to search tag).
     */
    public String generateTrapdoor(String keyword) {
        return generateSearchTag(keyword);
    }

    /**
     * Generates a set of deterministic search tags for a collection of extracted keywords.
     */
    public Set<String> generateSearchTags(Collection<String> keywords) {
        Objects.requireNonNull(keywords, "Keywords collection must not be null");
        return keywords.stream()
                .map(this::normalize)
                .filter(k -> !k.isEmpty())
                .map(this::generateSearchTag)
                .collect(Collectors.toSet());
    }

    /**
     * Computes HMAC-SHA256 using a specific key for custom/isolated tag computations.
     */
    public String generateSearchTag(String keyword, byte[] customKey) {
        Objects.requireNonNull(customKey, "Custom key must not be null");
        SecretKeySpec keySpec = new SecretKeySpec(customKey, HMAC_ALGORITHM);
        return computeHmac(normalize(keyword), keySpec);
    }

    private String computeHmac(String data, SecretKeySpec keySpec) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptographicException("Failed to compute HMAC-SHA256 search tag", e);
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        if (this.masterKeySpec == null) {
            throw new CryptographicException("SSE master key has not been configured. Set app.crypto.sse-master-key.");
        }
        return this.masterKeySpec;
    }
}
