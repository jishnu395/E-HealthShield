package com.ehealthshield.backend.crypto;

import com.ehealthshield.backend.exception.CryptographicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MlKemCryptoServiceTest {

    private MlKemCryptoService mlKemCryptoService;

    @BeforeEach
    void setUp() {
        mlKemCryptoService = new MlKemCryptoService();
    }

    @Test
    @DisplayName("1. testMlKem768KeyGeneration - Key pair is correctly generated with NIST FIPS 203 sizes")
    void testMlKem768KeyGeneration() {
        MlKemKeyPair keyPair = mlKemCryptoService.generateKeyPair();

        assertNotNull(keyPair, "Key pair must not be null");
        assertNotNull(keyPair.publicKey(), "Public key must not be null");
        assertNotNull(keyPair.privateKey(), "Private key must not be null");

        assertEquals(MlKemCryptoService.PUBLIC_KEY_SIZE_BYTES, keyPair.publicKey().length,
                "ML-KEM-768 public key size must be exactly 1184 bytes");
        assertEquals(MlKemCryptoService.PRIVATE_KEY_SIZE_BYTES, keyPair.privateKey().length,
                "ML-KEM-768 private key size must be exactly 2400 bytes");
    }

    @Test
    @DisplayName("2. testEncapsulationDecapsulationRoundTrip - Encapsulation and decapsulation round trip succeeds")
    void testEncapsulationDecapsulationRoundTrip() {
        MlKemKeyPair keyPair = mlKemCryptoService.generateKeyPair();

        MlKemEncapsulationResult encapsResult = mlKemCryptoService.encapsulate(keyPair.publicKey());

        assertNotNull(encapsResult);
        assertNotNull(encapsResult.ciphertext());
        assertNotNull(encapsResult.sharedSecret());

        assertEquals(MlKemCryptoService.CIPHERTEXT_SIZE_BYTES, encapsResult.ciphertext().length,
                "ML-KEM-768 ciphertext must be exactly 1088 bytes");
        assertEquals(MlKemCryptoService.SHARED_SECRET_SIZE_BYTES, encapsResult.sharedSecret().length,
                "Shared secret must be exactly 32 bytes (256 bits)");

        byte[] decapsulatedSecret = mlKemCryptoService.decapsulate(keyPair.privateKey(), encapsResult.ciphertext());

        assertNotNull(decapsulatedSecret);
        assertArrayEquals(encapsResult.sharedSecret(), decapsulatedSecret,
                "Decapsulated shared secret must match encapsulated shared secret");
    }

    @Test
    @DisplayName("3. testSharedSecretEquality - encapsulate(publicKey).sharedSecret == decapsulate(privateKey, ciphertext).sharedSecret")
    void testSharedSecretEquality() {
        MlKemKeyPair keyPair = mlKemCryptoService.generateKeyPair();

        MlKemEncapsulationResult senderResult = mlKemCryptoService.encapsulate(keyPair.publicKey());
        byte[] receiverSecret = mlKemCryptoService.decapsulate(keyPair.privateKey(), senderResult.ciphertext());

        assertArrayEquals(senderResult.sharedSecret(), receiverSecret,
                "sender.sharedSecret == receiver.sharedSecret (NIST FIPS 203 invariant)");
    }

    @Test
    @DisplayName("4. testDifferentEncapsulationsProduceDifferentCiphertexts - Distinct encapsulations generate distinct ciphertexts and shared secrets")
    void testDifferentEncapsulationsProduceDifferentCiphertexts() {
        MlKemKeyPair keyPair = mlKemCryptoService.generateKeyPair();

        MlKemEncapsulationResult encaps1 = mlKemCryptoService.encapsulate(keyPair.publicKey());
        MlKemEncapsulationResult encaps2 = mlKemCryptoService.encapsulate(keyPair.publicKey());

        assertFalse(Arrays.equals(encaps1.ciphertext(), encaps2.ciphertext()),
                "Consecutive encapsulations must produce randomized, unique ciphertexts");
        assertFalse(Arrays.equals(encaps1.sharedSecret(), encaps2.sharedSecret()),
                "Consecutive encapsulations must produce distinct ephemeral shared secrets");
    }

    @Test
    @DisplayName("5. testWrongPrivateKeyFails - Decapsulating with an incorrect private key does not recover original shared secret")
    void testWrongPrivateKeyFails() {
        MlKemKeyPair keyPair1 = mlKemCryptoService.generateKeyPair();
        MlKemKeyPair keyPair2 = mlKemCryptoService.generateKeyPair();

        MlKemEncapsulationResult encapsResult = mlKemCryptoService.encapsulate(keyPair1.publicKey());

        // Decapsulate using keyPair2's private key (wrong key)
        byte[] decapsulatedWithWrongKey = mlKemCryptoService.decapsulate(keyPair2.privateKey(), encapsResult.ciphertext());

        assertFalse(Arrays.equals(encapsResult.sharedSecret(), decapsulatedWithWrongKey),
                "Decapsulating with wrong private key must not yield the sender's shared secret (Fujisaki-Okamoto implicit rejection)");
    }

    @Test
    @DisplayName("6. testInvalidCiphertextFails - Invalid key or ciphertext lengths throw CryptographicException; corrupted ciphertext yields implicit rejection")
    void testInvalidCiphertextFails() {
        MlKemKeyPair keyPair = mlKemCryptoService.generateKeyPair();

        // 1. Invalid Public Key Length
        byte[] truncatedPubKey = new byte[500];
        assertThrows(CryptographicException.class, () -> mlKemCryptoService.encapsulate(truncatedPubKey));

        // 2. Invalid Private Key Length
        byte[] truncatedPrivKey = new byte[500];
        byte[] validCiphertext = new byte[MlKemCryptoService.CIPHERTEXT_SIZE_BYTES];
        assertThrows(CryptographicException.class, () -> mlKemCryptoService.decapsulate(truncatedPrivKey, validCiphertext));

        // 3. Invalid Ciphertext Length
        byte[] truncatedCiphertext = new byte[500];
        assertThrows(CryptographicException.class, () -> mlKemCryptoService.decapsulate(keyPair.privateKey(), truncatedCiphertext));

        // 4. Corrupted Ciphertext (Flipping bits) -> Fujisaki-Okamoto transform yields pseudo-random secret, not original
        MlKemEncapsulationResult encapsResult = mlKemCryptoService.encapsulate(keyPair.publicKey());
        byte[] corruptedCiphertext = Arrays.copyOf(encapsResult.ciphertext(), encapsResult.ciphertext().length);
        corruptedCiphertext[0] ^= 0xFF;

        byte[] decapsulatedCorrupted = mlKemCryptoService.decapsulate(keyPair.privateKey(), corruptedCiphertext);
        assertFalse(Arrays.equals(encapsResult.sharedSecret(), decapsulatedCorrupted),
                "Corrupted ciphertext must not yield the original shared secret");
    }
}
