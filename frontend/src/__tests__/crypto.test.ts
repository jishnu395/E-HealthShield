import { describe, it, expect } from 'vitest';
import { ml_kem768 } from '@noble/post-quantum/ml-kem.js';
import {
  base64ToBuffer,
  bufferToBase64,
  decapsulateAesKey,
  decryptAesGcmPayload,
  decryptEhrBundle
} from '../utils/cryptoUtils';
import { EhrRetrievalResponse } from '../types/ehr';

describe('Phase 5 Client-Side Cryptography Test Suite', () => {
  it('1. base64ToBuffer and bufferToBase64 correctly encode and decode byte arrays', () => {
    const originalBytes = new Uint8Array([72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100]); // "Hello World"
    const base64 = bufferToBase64(originalBytes);
    expect(base64).toBe('SGVsbG8gV29ybGQ=');

    const decodedBytes = base64ToBuffer(base64);
    expect(decodedBytes).toEqual(originalBytes);
  });

  it('2. ML-KEM-768 decapsulation recovers original 32-byte shared secret', () => {
    // Generate valid ML-KEM-768 key pair using @noble/post-quantum
    const keys = ml_kem768.keygen();
    expect(keys.publicKey.length).toBe(1184);
    expect(keys.secretKey.length).toBe(2400);

    // Encapsulate 32-byte secret
    const encaps = ml_kem768.encapsulate(keys.publicKey);
    expect(encaps.cipherText.length).toBe(1088);
    expect(encaps.sharedSecret.length).toBe(32);

    // Decapsulate using decapsulateAesKey helper
    const kemCiphertextBase64 = bufferToBase64(encaps.cipherText);
    const recoveredSecret = decapsulateAesKey(kemCiphertextBase64, keys.secretKey);

    expect(recoveredSecret.length).toBe(32);
    expect(recoveredSecret).toEqual(encaps.sharedSecret);
  });

  it('3. Web Crypto API AES-256-GCM decrypts payload byte-for-byte', async () => {
    // Generate a 32-byte AES key and 12-byte IV
    const aesKeyBytes = new Uint8Array(32);
    crypto.getRandomValues(aesKeyBytes);

    const ivBytes = new Uint8Array(12);
    crypto.getRandomValues(ivBytes);

    const plaintext = new TextEncoder().encode('Confidential E2E Health Report: Patient Diagnosis Healthy.');

    // Encrypt using Web Crypto API
    const cryptoKey = await crypto.subtle.importKey('raw', aesKeyBytes, { name: 'AES-GCM' }, false, ['encrypt']);
    const encryptedBuffer = await crypto.subtle.encrypt({ name: 'AES-GCM', iv: ivBytes, tagLength: 128 }, cryptoKey, plaintext);
    const ciphertextBase64 = bufferToBase64(new Uint8Array(encryptedBuffer));
    const ivBase64 = bufferToBase64(ivBytes);

    // Decrypt using decryptAesGcmPayload
    const decryptedBytes = await decryptAesGcmPayload(ciphertextBase64, ivBase64, aesKeyBytes);
    const recoveredText = new TextDecoder().decode(decryptedBytes);

    expect(recoveredText).toBe('Confidential E2E Health Report: Patient Diagnosis Healthy.');
  });

  it('4. Handles invalid/truncated private key length gracefully', () => {
    const invalidPrivKey = new Uint8Array(100); // Invalid length != 2400
    const dummyKemCiphertext = bufferToBase64(new Uint8Array(1088));

    expect(() => decapsulateAesKey(dummyKemCiphertext, invalidPrivKey)).toThrowError(
      'Invalid ML-KEM-768 private key length'
    );
  });

  it('5. Handles invalid KEM ciphertext length gracefully', () => {
    const keys = ml_kem768.keygen();
    const truncatedKemCiphertextBase64 = bufferToBase64(new Uint8Array(500)); // Invalid length != 1088

    expect(() => decapsulateAesKey(truncatedKemCiphertextBase64, keys.secretKey)).toThrowError(
      'Invalid ML-KEM-768 ciphertext length'
    );
  });

  it('6. Rejects tampered ciphertext or modified authentication tag in AES-256-GCM', async () => {
    const aesKeyBytes = new Uint8Array(32);
    crypto.getRandomValues(aesKeyBytes);

    const ivBytes = new Uint8Array(12);
    crypto.getRandomValues(ivBytes);

    const plaintext = new TextEncoder().encode('Secret Data');

    const cryptoKey = await crypto.subtle.importKey('raw', aesKeyBytes, { name: 'AES-GCM' }, false, ['encrypt']);
    const encryptedBuffer = await crypto.subtle.encrypt({ name: 'AES-GCM', iv: ivBytes, tagLength: 128 }, cryptoKey, plaintext);

    // Tamper single bit in ciphertext / auth tag
    const ciphertextBytes = new Uint8Array(encryptedBuffer);
    ciphertextBytes[0] ^= 0xff;
    const tamperedCiphertextBase64 = bufferToBase64(ciphertextBytes);
    const ivBase64 = bufferToBase64(ivBytes);

    await expect(decryptAesGcmPayload(tamperedCiphertextBase64, ivBase64, aesKeyBytes)).rejects.toThrowError(
      'AES-256-GCM decryption failed'
    );
  });

  it('7. decryptEhrBundle executes end-to-end decapsulation and decryption on EHR bundle', async () => {
    const keys = ml_kem768.keygen();
    const encaps = ml_kem768.encapsulate(keys.publicKey);

    const aesKeyBytes = encaps.sharedSecret;
    const ivBytes = new Uint8Array(12);
    crypto.getRandomValues(ivBytes);

    const plaintextStr = 'Clinical EHR Payload: Complete recovery test.';
    const plaintextBytes = new TextEncoder().encode(plaintextStr);

    const cryptoKey = await crypto.subtle.importKey('raw', aesKeyBytes, { name: 'AES-GCM' }, false, ['encrypt']);
    const encryptedBuffer = await crypto.subtle.encrypt({ name: 'AES-GCM', iv: ivBytes, tagLength: 128 }, cryptoKey, plaintextBytes);

    const mockBundle: EhrRetrievalResponse = {
      recordId: '123e4567-e89b-12d3-a456-426614174000',
      fileName: 'test_clinical_report.txt',
      contentType: 'text/plain',
      patientWallet: '0x70997970C51812dc3A010C7d01b50e0d17dc79C8',
      uploaderWallet: '0x90F79bf6EB2c4f870365E785982E1f101E93b906',
      encryptedCiphertext: bufferToBase64(new Uint8Array(encryptedBuffer)),
      iv: bufferToBase64(ivBytes),
      kemCiphertext: bufferToBase64(encaps.cipherText),
      fileHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
      createdAt: new Date().toISOString()
    };

    const decrypted = await decryptEhrBundle(mockBundle, keys.secretKey);
    expect(decrypted.fileName).toBe('test_clinical_report.txt');
    expect(decrypted.contentType).toBe('text/plain');

    const recoveredText = new TextDecoder().decode(decrypted.data);
    expect(recoveredText).toBe(plaintextStr);
  });
});
