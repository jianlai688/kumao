package com.kumao.review.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretCipherTest {

    private final SecretCipher cipher = new SecretCipher("unit-test-secret");

    @Test
    void shouldEncryptAndDecryptApiKey() {
        String encrypted = cipher.encrypt("sk-1234567890abcdef");

        assertTrue(encrypted.startsWith("enc:v1:"));
        assertNotEquals("sk-1234567890abcdef", encrypted);
        assertEquals("sk-1234567890abcdef", cipher.decrypt(encrypted));
    }

    @Test
    void shouldOnlyExposeMaskedKey() {
        String masked = cipher.mask("sk-1234567890abcdef");

        assertEquals("sk-****cdef", masked);
        assertFalse(masked.contains("1234567890"));
    }

    @Test
    void shouldKeepLegacyPlainTextReadable() {
        assertEquals("sk-legacy", cipher.decrypt("sk-legacy"));
    }
}
