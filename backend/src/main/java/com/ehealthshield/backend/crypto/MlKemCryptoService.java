package com.ehealthshield.backend.crypto;

import com.ehealthshield.backend.exception.CryptographicException;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Objects;

@Service
public class MlKemCryptoService {

    public static final MLKEMParameters DEFAULT_PARAMETERS = MLKEMParameters.ml_kem_768;
    public static final int PUBLIC_KEY_SIZE_BYTES = 1184;
    public static final int PRIVATE_KEY_SIZE_BYTES = 2400;
    public static final int CIPHERTEXT_SIZE_BYTES = 1088;
    public static final int SHARED_SECRET_SIZE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates an ML-KEM-768 post-quantum key pair according to NIST FIPS 203.
     */
    public MlKemKeyPair generateKeyPair() {
        try {
            MLKEMKeyPairGenerator generator = new MLKEMKeyPairGenerator();
            generator.init(new MLKEMKeyGenerationParameters(secureRandom, DEFAULT_PARAMETERS));
            AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();

            MLKEMPublicKeyParameters pubKey = (MLKEMPublicKeyParameters) keyPair.getPublic();
            MLKEMPrivateKeyParameters privKey = (MLKEMPrivateKeyParameters) keyPair.getPrivate();

            return new MlKemKeyPair(pubKey.getEncoded(), privKey.getEncoded());
        } catch (Exception e) {
            throw new CryptographicException("Failed to generate ML-KEM-768 key pair", e);
        }
    }

    /**
     * Encapsulates a new shared secret against the provided ML-KEM-768 public key bytes.
     */
    public MlKemEncapsulationResult encapsulate(byte[] publicKeyBytes) {
        Objects.requireNonNull(publicKeyBytes, "Public key bytes must not be null");
        if (publicKeyBytes.length != PUBLIC_KEY_SIZE_BYTES) {
            throw new CryptographicException("Invalid ML-KEM-768 public key size: expected "
                    + PUBLIC_KEY_SIZE_BYTES + " bytes, got " + publicKeyBytes.length);
        }

        try {
            MLKEMPublicKeyParameters pubKeyParams = new MLKEMPublicKeyParameters(DEFAULT_PARAMETERS, publicKeyBytes);
            MLKEMGenerator generator = new MLKEMGenerator(secureRandom);
            SecretWithEncapsulation secretWithEncaps = generator.generateEncapsulated(pubKeyParams);

            return new MlKemEncapsulationResult(
                    secretWithEncaps.getEncapsulation(),
                    secretWithEncaps.getSecret()
            );
        } catch (Exception e) {
            throw new CryptographicException("ML-KEM-768 encapsulation failed", e);
        }
    }

    /**
     * Decapsulates the shared secret from the encapsulated ciphertext using the ML-KEM-768 private key bytes.
     */
    public byte[] decapsulate(byte[] privateKeyBytes, byte[] encapsulatedCiphertext) {
        Objects.requireNonNull(privateKeyBytes, "Private key bytes must not be null");
        Objects.requireNonNull(encapsulatedCiphertext, "Encapsulated ciphertext must not be null");

        if (privateKeyBytes.length != PRIVATE_KEY_SIZE_BYTES) {
            throw new CryptographicException("Invalid ML-KEM-768 private key size: expected "
                    + PRIVATE_KEY_SIZE_BYTES + " bytes, got " + privateKeyBytes.length);
        }
        if (encapsulatedCiphertext.length != CIPHERTEXT_SIZE_BYTES) {
            throw new CryptographicException("Invalid ML-KEM-768 ciphertext size: expected "
                    + CIPHERTEXT_SIZE_BYTES + " bytes, got " + encapsulatedCiphertext.length);
        }

        try {
            MLKEMPrivateKeyParameters privKeyParams = new MLKEMPrivateKeyParameters(DEFAULT_PARAMETERS, privateKeyBytes);
            MLKEMExtractor extractor = new MLKEMExtractor(privKeyParams);
            return extractor.extractSecret(encapsulatedCiphertext);
        } catch (Exception e) {
            throw new CryptographicException("ML-KEM-768 decapsulation failed", e);
        }
    }
}
