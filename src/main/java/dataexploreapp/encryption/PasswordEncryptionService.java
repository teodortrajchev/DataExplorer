package dataexploreapp.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordEncryptionService {

    private static final Path DATA_DIR =
            Path.of(System.getProperty("user.home"), ".dataexplorer");

    private static final Path KEY_FILE =
            DATA_DIR.resolve("connection.key");

    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static SecretKey getOrCreateKey() throws Exception {

        Files.createDirectories(DATA_DIR);

        if (Files.exists(KEY_FILE)) {
            byte[] encodedKey = Files.readAllBytes(KEY_FILE);
            return new SecretKeySpec(encodedKey, "AES");
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);

        SecretKey key = keyGenerator.generateKey();

        Files.write(KEY_FILE, key.getEncoded());

        return key;
    }

    public static String encrypt(String password) throws Exception {

        SecretKey key = getOrCreateKey();

        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] encrypted =
                cipher.doFinal(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        byte[] result = new byte[iv.length + encrypted.length];

        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(result);
    }

    public static String decrypt(String encryptedPassword) throws Exception {

        SecretKey key = getOrCreateKey();

        byte[] data =
                Base64.getDecoder().decode(encryptedPassword);

        byte[] iv = new byte[IV_LENGTH];

        byte[] encrypted =
                new byte[data.length - IV_LENGTH];

        System.arraycopy(data, 0, iv, 0, IV_LENGTH);
        System.arraycopy(
                data,
                IV_LENGTH,
                encrypted,
                0,
                encrypted.length
        );

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        GCMParameterSpec spec =
                new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decrypted =
                cipher.doFinal(encrypted);

        return new String(
                decrypted,
                java.nio.charset.StandardCharsets.UTF_8
        );
    }
}