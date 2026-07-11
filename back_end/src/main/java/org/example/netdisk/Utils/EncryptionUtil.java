package org.example.netdisk.Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 加密 / 解密工具 — XOR 流密码实现
 *
 * 原理：用 SHA-256 从密码+随机盐值派生密钥流，逐字节与数据 XOR。
 * 输出格式：[salt(8B)][ciphertext]，其中 salt 是每次加密随机生成的，
 * 确保同一密码加密不同数据的密钥流不同。
 */
public class EncryptionUtil {

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    /**
     * 加密数据
     * @param data     原始字节
     * @param password 密码
     * @return 加密后的字节（应包含解密所需的所有信息，如盐值、IV 等）
     */
    public static byte[] encrypt(byte[] data, String password) {
        if (data == null || data.length == 0) return new byte[0];

        // 1. 生成随机盐值
        byte[] salt = new byte[8];
        SECURE_RANDOM.nextBytes(salt);

        // 2. 派生密钥流
        byte[] keystream = deriveKeystream(password, salt, data.length);

        // 3. 逐字节 XOR
        byte[] encrypted = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            encrypted[i] = (byte) (data[i] ^ keystream[i]);
        }

        // 4. 组装: salt(8) + 密文
        byte[] result = new byte[8 + encrypted.length];
        System.arraycopy(salt, 0, result, 0, 8);
        System.arraycopy(encrypted, 0, result, 8, encrypted.length);
        return result;
    }

    /**
     * 解密数据
     * @param data     加密后的字节
     * @param password 密码
     * @return 解密后的原始字节
     */
    public static byte[] decrypt(byte[] data, String password) {
        if (data == null || data.length < 8) return new byte[0];

        // 1. 拆包: 前 8 字节是盐值
        byte[] salt = Arrays.copyOfRange(data, 0, 8);
        byte[] ciphertext = Arrays.copyOfRange(data, 8, data.length);

        // 2. 派生完全相同的密钥流
        byte[] keystream = deriveKeystream(password, salt, ciphertext.length);

        // 3. XOR 解密（和加密一模一样）
        byte[] plaintext = new byte[ciphertext.length];
        for (int i = 0; i < ciphertext.length; i++) {
            plaintext[i] = (byte) (ciphertext[i] ^ keystream[i]);
        }
        return plaintext;
    }

    private static byte[] deriveKeystream(String password, byte[] salt, int length) {
        byte[] keystream = new byte[length];
        byte[] pwdBytes = password.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            int generated = 0;
            int counter = 0;
            while (generated < length) {
                md.update(pwdBytes);
                md.update(salt);
                md.update(new byte[] {
                        (byte) (counter >> 24),
                        (byte) (counter >> 16),
                        (byte) (counter >> 8),
                        (byte) counter
                });
                byte[] hash = md.digest();
                int toCopy = Math.min(hash.length, length - generated);
                System.arraycopy(hash, 0, keystream, generated, toCopy);
                generated += toCopy;
                counter++;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
        return keystream;
    }
}