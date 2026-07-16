package org.example.netdisk.Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class EncryptionUtil {

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom(); // 密码学安全随机数，用于生成盐值

    public static byte[] encrypt(byte[] data, String password) { // 使用XOR流密码加密数据，返回[盐值8字节][密文]格式
        if (data == null || data.length == 0) return new byte[0]; // 空数据不处理

        byte[] salt = new byte[8]; // 8字节随机盐值确保每次加密密钥流不同
        SECURE_RANDOM.nextBytes(salt); // 填充随机盐值

        byte[] keystream = deriveKeystream(password, salt, data.length); // 根据密码和盐值派生与数据等长的密钥流

        byte[] encrypted = new byte[data.length]; // 密文数组
        for (int i = 0; i < data.length; i++) { // 逐字节XOR加密
            encrypted[i] = (byte) (data[i] ^ keystream[i]); // XOR操作：相同密钥流加密解密结果一致
        }

        byte[] result = new byte[8 + encrypted.length]; // 最终输出：[8字节盐值] + [密文]
        System.arraycopy(salt, 0, result, 0, 8); // 将盐值写入前8字节
        System.arraycopy(encrypted, 0, result, 8, encrypted.length); // 将密文写入剩余部分
        return result;
    }

    public static byte[] decrypt(byte[] data, String password) { // 解密数据，输入格式须为[盐值8字节][密文]
        if (data == null || data.length < 8) return new byte[0]; // 数据不足8字节（连盐值都不完整）则无法解密

        byte[] salt = Arrays.copyOfRange(data, 0, 8); // 提取前8字节盐值
        byte[] ciphertext = Arrays.copyOfRange(data, 8, data.length); // 提取剩余密文

        byte[] keystream = deriveKeystream(password, salt, ciphertext.length); // 用相同盐值和密码派生密钥流

        byte[] plaintext = new byte[ciphertext.length]; // 明文数组
        for (int i = 0; i < ciphertext.length; i++) { // 逐字节XOR解密（与加密完全对称）
            plaintext[i] = (byte) (ciphertext[i] ^ keystream[i]); // XOR解密：密文 XOR 密钥流 = 明文
        }
        return plaintext;
    }

    private static byte[] deriveKeystream(String password, byte[] salt, int length) { // 用SHA-256从密码+盐值+计数器派生任意长度的密钥流
        byte[] keystream = new byte[length]; // 密钥流数组
        byte[] pwdBytes = password.getBytes(StandardCharsets.UTF_8); // 密码转为字节数组
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256"); // 使用SHA-256哈希算法
            int generated = 0; // 已生成的密钥流字节数
            int counter = 0; // 计数器，当单次SHA-256输出不足时递增以产生更多哈希值
            while (generated < length) { // 循环直到生成足够长度的密钥流
                md.update(pwdBytes); // 加入密码
                md.update(salt); // 加入盐值
                md.update(new byte[] { // 加入4字节计数器确保每次哈希输出不同
                        (byte) (counter >> 24), // 计数器第1字节（高位）
                        (byte) (counter >> 16), // 计数器第2字节
                        (byte) (counter >> 8), // 计数器第3字节
                        (byte) counter // 计数器第4字节（低位）
                });
                byte[] hash = md.digest(); // 计算SHA-256哈希值
                int toCopy = Math.min(hash.length, length - generated); // 本次可复制的字节数
                System.arraycopy(hash, 0, keystream, generated, toCopy); // 将哈希值复制到密钥流
                generated += toCopy; // 更新已生成计数
                counter++; // 递增计数器以产生不同的哈希值
            }
        } catch (NoSuchAlgorithmException e) { // SHA-256算法不可用时抛出异常
            throw new RuntimeException("SHA-256 不可用", e);
        }
        return keystream;
    }
}
