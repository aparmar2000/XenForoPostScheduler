package aparmar2000.xenforoposter.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for encrypting and decrypting sensitive credentials using AES-256-GCM authenticated encryption.
 */
@Slf4j
public class CredentialEncryptionService {
    public static final String PREFIX = "ENC:";
    private static final String AES_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int KEY_SIZE_BITS = 256;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(@NotNull Path keyFile) {
        this.secretKey = loadOrCreateKey(keyFile);
    }

    public CredentialEncryptionService(@NotNull SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public static CredentialEncryptionService createInMemory() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(KEY_SIZE_BITS);
            return new CredentialEncryptionService(keyGen.generateKey());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate in-memory AES key", e);
        }
    }

    private SecretKey loadOrCreateKey(@NotNull Path keyFile) {
        try {
            if (Files.exists(keyFile)) {
                byte[] keyBytes = Files.readAllBytes(keyFile);
                if (keyBytes.length == 32 || keyBytes.length == 16) {
                    return new SecretKeySpec(keyBytes, AES_ALGORITHM);
                }
                log.warn("Existing key file {} has invalid length ({}), generating new key", keyFile, keyBytes.length);
            }

            if (keyFile.getParent() != null) {
                Files.createDirectories(keyFile.getParent());
            }

            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(KEY_SIZE_BITS);
            SecretKey generatedKey = keyGen.generateKey();
            Files.write(keyFile, generatedKey.getEncoded());
            return generatedKey;
        } catch (IOException e) {
            log.error("Failed to load or save key file at {}, generating fallback in-memory key", keyFile, e);
            return createInMemory().secretKey;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES key from " + keyFile, e);
        }
    }

    /**
     * Checks whether a string conforms to the encrypted ciphertext format.
     */
    public static boolean isEncrypted(@Nullable String text) {
        return text != null && text.startsWith(PREFIX);
    }

    /**
     * Encrypts a cleartext string using AES-256-GCM.
     *
     * @param plainText the plaintext to encrypt
     * @return the encrypted string prefixed with {@value #PREFIX}, or null if plainText is null
     */
    public @Nullable String encrypt(@Nullable String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Error encrypting sensitive data", e);
            throw new IllegalStateException("Failed to encrypt sensitive data", e);
        }
    }

    /**
     * Decrypts an encrypted string produced by {@link #encrypt(String)}.
     *
     * @param cipherText the ciphertext starting with {@value #PREFIX}
     * @return the decrypted cleartext string, or null if cipherText is null
     */
    public @Nullable String decrypt(@Nullable String cipherText) {
        if (cipherText == null) {
            return null;
        }

        if (!isEncrypted(cipherText)) {
            throw new IllegalArgumentException("Payload does not match encrypted format: missing " + PREFIX);
        }

        try {
            String rawBase64 = cipherText.substring(PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(rawBase64);

            if (combined.length < GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Ciphertext payload is too short to contain IV");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] cipherBytes = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting sensitive data", e);
            throw new IllegalStateException("Failed to decrypt sensitive data", e);
        }
    }
}
