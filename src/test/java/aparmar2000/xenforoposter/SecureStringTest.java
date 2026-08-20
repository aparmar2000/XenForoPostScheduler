package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aparmar2000.xenforoposter.security.CredentialEncryptionService;
import aparmar2000.xenforoposter.security.SecureString;

class SecureStringTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("SecureString toString should not leak plaintext")
    void testToStringOutput() {
    	String secretString = "superSecretPassword123!";
        SecureString secure = SecureString.of(secretString);

        assertNotNull(secure);
        assertFalse(secure.toString().contains(secretString));
        assertEquals(secretString, secure.getClearText());
    }

    @Test
    @DisplayName("SecureString utility methods should handle char arrays, length, emptiness, and blanks")
    void testUtilityMethods() {
        SecureString fromChars = SecureString.of(new char[]{'p', 'a', 's', 's'});
        assertNotNull(fromChars);
        assertEquals("pass", fromChars.getClearText());
        assertEquals(4, fromChars.length());
        assertFalse(fromChars.isEmpty());
        assertFalse(fromChars.isBlank());

        SecureString empty = SecureString.of("");
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        assertTrue(empty.isBlank());
        assertEquals(0, empty.length());

        SecureString blank = SecureString.of("   ");
        assertNotNull(blank);
        assertFalse(blank.isEmpty());
        assertTrue(blank.isBlank());

        assertNull(SecureString.of((String) null));
        assertNull(SecureString.of((char[]) null));
    }

    @Test
    @DisplayName("SecureString equality and hashCode should be based on cleartext content")
    void testEqualityAndHashCode() {
        SecureString s1 = SecureString.of("secret");
        SecureString s2 = SecureString.of("secret");
        SecureString s3 = SecureString.of("different");

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3);
    }

    @Test
    @DisplayName("CredentialEncryptionService should perform AES-256-GCM encryption and decryption")
    void testCredentialEncryptionCycle() {
        CredentialEncryptionService crypto = CredentialEncryptionService.createInMemory();
        String sensitive = "mySuperSecretPassword@2026!";

        String encrypted = crypto.encrypt(sensitive);
        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC:"));
        assertFalse(encrypted.contains(sensitive));

        String decrypted = crypto.decrypt(encrypted);
        assertEquals(sensitive, decrypted);
    }

    @Test
    @DisplayName("CredentialEncryptionService should generate unique ciphertexts for identical plaintexts (random IV)")
    void testRandomIvUniqueness() {
        CredentialEncryptionService crypto = CredentialEncryptionService.createInMemory();
        String sensitive = "identicalCleartext";

        String enc1 = crypto.encrypt(sensitive);
        String enc2 = crypto.encrypt(sensitive);

        assertNotEquals(enc1, enc2, "Each encryption should use a fresh random IV");
        assertEquals(sensitive, crypto.decrypt(enc1));
        assertEquals(sensitive, crypto.decrypt(enc2));
    }

    @Test
    @DisplayName("CredentialEncryptionService should persist and reload master key from disk")
    void testKeyPersistence() {
        Path keyFile = tempDir.resolve(".credentials.key");
        CredentialEncryptionService crypto1 = new CredentialEncryptionService(keyFile);

        String secret = "testPersistentKeySecret";
        String ciphertext = crypto1.encrypt(secret);

        // Create second instance reading the same key file
        CredentialEncryptionService crypto2 = new CredentialEncryptionService(keyFile);
        String decrypted = crypto2.decrypt(ciphertext);

        assertEquals(secret, decrypted);
    }
}
