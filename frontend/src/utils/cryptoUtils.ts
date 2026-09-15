import { ml_kem768 } from '@noble/post-quantum/ml-kem.js';
import { EhrRetrievalResponse } from '../types/ehr';

export function base64ToBuffer(base64: string): Uint8Array {
  const binaryString = atob(base64.trim());
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

export function bufferToBase64(buffer: Uint8Array): string {
  let binary = '';
  const bytes = new Uint8Array(buffer);
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

/**
 * Decapsulates the 32-byte (256-bit) AES key from the 1088-byte ML-KEM-768 ciphertext
 * using the recipient's 2400-byte ML-KEM-768 secret key according to NIST FIPS 203.
 *
 * @param kemCiphertextBase64 Base64-encoded 1088-byte KEM ciphertext.
 * @param privateKeyBytes 2400-byte ML-KEM-768 private key array.
 * @returns 32-byte AES-256 key array.
 */
export function decapsulateAesKey(kemCiphertextBase64: string, privateKeyBytes: Uint8Array): Uint8Array {
  if (!kemCiphertextBase64) {
    throw new Error('KEM ciphertext is required for key decapsulation.');
  }
  if (!privateKeyBytes || privateKeyBytes.length !== 2400) {
    throw new Error(`Invalid ML-KEM-768 private key length: expected 2400 bytes, got ${privateKeyBytes?.length || 0}.`);
  }

  const kemCiphertextBytes = base64ToBuffer(kemCiphertextBase64);
  if (kemCiphertextBytes.length !== 1088) {
    throw new Error(`Invalid ML-KEM-768 ciphertext length: expected 1088 bytes, got ${kemCiphertextBytes.length}.`);
  }

  try {
    // Decapsulate using @noble/post-quantum NIST FIPS 203 implementation
    return ml_kem768.decapsulate(kemCiphertextBytes, privateKeyBytes);
  } catch (err: any) {
    throw new Error(`ML-KEM-768 decapsulation failed: ${err.message || 'Invalid key or ciphertext'}`);
  }
}

/**
 * Decrypts AES-256-GCM encrypted payload using Web Crypto API.
 *
 * @param encryptedCiphertextBase64 Base64-encoded ciphertext (includes appended 16-byte GCM auth tag).
 * @param ivBase64 Base64-encoded 12-byte (96-bit) IV.
 * @param aesKeyBytes 32-byte AES-256 key array.
 * @returns Decrypted plaintext payload bytes.
 */
export async function decryptAesGcmPayload(
  encryptedCiphertextBase64: string,
  ivBase64: string,
  aesKeyBytes: Uint8Array
): Promise<Uint8Array> {
  if (!encryptedCiphertextBase64) {
    throw new Error('Encrypted ciphertext is required for decryption.');
  }
  if (!ivBase64) {
    throw new Error('Initialization vector (IV) is required for AES-GCM decryption.');
  }
  if (!aesKeyBytes || aesKeyBytes.length !== 32) {
    throw new Error(`Invalid AES key length: expected 32 bytes (256 bits), got ${aesKeyBytes?.length || 0}.`);
  }

  const ciphertextBytes = base64ToBuffer(encryptedCiphertextBase64);
  const ivBytes = base64ToBuffer(ivBase64);

  if (ivBytes.length !== 12) {
    throw new Error(`Invalid AES-GCM IV length: expected 12 bytes (96 bits), got ${ivBytes.length}.`);
  }

  try {
    // Import raw 256-bit key into Web Crypto API
    const cryptoKey = await window.crypto.subtle.importKey(
      'raw',
      aesKeyBytes.buffer.slice(aesKeyBytes.byteOffset, aesKeyBytes.byteOffset + aesKeyBytes.byteLength) as ArrayBuffer,
      { name: 'AES-GCM' },
      false,
      ['decrypt']
    );

    // Decrypt AES-256-GCM ciphertext
    const decryptedBuffer = await window.crypto.subtle.decrypt(
      {
        name: 'AES-GCM',
        iv: ivBytes.buffer.slice(ivBytes.byteOffset, ivBytes.byteOffset + ivBytes.byteLength) as ArrayBuffer,
        tagLength: 128
      },
      cryptoKey,
      ciphertextBytes.buffer.slice(ciphertextBytes.byteOffset, ciphertextBytes.byteOffset + ciphertextBytes.byteLength) as ArrayBuffer
    );

    return new Uint8Array(decryptedBuffer);
  } catch (err: any) {
    throw new Error('AES-256-GCM decryption failed: authentication tag mismatch or corrupted ciphertext data.');
  }
}

/**
 * End-to-end client-side EHR decryption pipeline:
 * 1. Decapsulates 32-byte AES key using ML-KEM-768 private key.
 * 2. Decrypts AES-256-GCM ciphertext payload via Web Crypto API.
 * 3. Returns original plaintext bytes, file name, and content type.
 */
export async function decryptEhrBundle(
  bundle: EhrRetrievalResponse,
  privateKeyBytes: Uint8Array
): Promise<{ fileName: string; contentType: string; data: Uint8Array }> {
  if (!bundle) {
    throw new Error('EHR retrieval bundle is null or undefined.');
  }

  // 1. Decapsulate AES key
  const aesKey = decapsulateAesKey(bundle.kemCiphertext, privateKeyBytes);

  // 2. Decrypt AES-256-GCM ciphertext payload
  const plaintextData = await decryptAesGcmPayload(bundle.encryptedCiphertext, bundle.iv, aesKey);

  return {
    fileName: bundle.fileName || 'decrypted_ehr_report.txt',
    contentType: bundle.contentType || 'text/plain',
    data: plaintextData
  };
}
