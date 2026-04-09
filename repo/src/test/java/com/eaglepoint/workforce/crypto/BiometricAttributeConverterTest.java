package com.eaglepoint.workforce.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BiometricAttributeConverterTest {

    private final BiometricAttributeConverter converter;

    BiometricAttributeConverterTest() {
        // Ensure key is set for tests (no hardcoded fallback in production code)
        System.setProperty("app.encryption.key", "dGhpcy1pcy1hLTMyLWJ5dGUta2V5ISEhMTIzNDU2Nzg=");
        converter = new BiometricAttributeConverter();
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        byte[] plaintext = "biometric-template-data-sample-12345".getBytes();

        byte[] encrypted = converter.convertToDatabaseColumn(plaintext);
        assertNotNull(encrypted);
        assertNotEquals(new String(plaintext), new String(encrypted));

        byte[] decrypted = converter.convertToEntityAttribute(encrypted);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void nullInputReturnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void differentEncryptionsProduceDifferentCiphertext() {
        byte[] plaintext = "same-data".getBytes();
        byte[] encrypted1 = converter.convertToDatabaseColumn(plaintext);
        byte[] encrypted2 = converter.convertToDatabaseColumn(plaintext);

        // Due to random IV, ciphertexts should differ
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2));
    }
}
