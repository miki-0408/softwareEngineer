package org.example.netdisk.Utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 手动实现的 Feistel 网络分组密码
 *
 * ██████  设计原理  ██████
 *
 * Feistel 网络是分组密码的经典结构（DES 使用的就是它），核心优势：
 *   无论轮函数 F 多么简单，加解密代码完全对称 —— 解密只用把子密钥逆序。
 *
 * ┌─────────────────────────────────────────────────────┐
 * │                     加密                             │
 * │  plaintext (16 字节)                                 │
 * │       │                                              │
 * │  L0 ──┤├── R0   (各 8 字节)                          │
 * │  for round = 0..15:                                 │
 * │    Lᵢ₊₁ = Rᵢ                                        │
 * │    Rᵢ₊₁ = Lᵢ ⊕ F(Rᵢ, subKey[round])                 │
 * │       │                                              │
 * │  ciphertext = L₁₆ || R₁₆                             │
 * │                                                      │
 * │                     解密                             │
 * │  相同流程，但 subKey 顺序反过来：round = 15..0       │
 * └─────────────────────────────────────────────────────┘
 *
 * ██████  本实现参数  ██████
 *   - 分组大小: 16 字节 (128 位)
 *   - 轮数: 16
 *   - 子密钥: 每轮 8 字节，从密码通过 SHA-256 派生
 *   - 轮函数 F: XOR ⊕ SubBytes(仿射变换 S 盒)
 *   - 填充: PKCS7
 *
 * ██████  安全性说明  ██████
 *   这是一个用于学习的参考实现，展示了 Feistel 网络的核心结构。
 *   你可以替换轮函数 F 来增强安全性（例如加入 P 盒置换、更复杂的 S 盒等）。
 *   尚未达到生产级安全强度，请勿用于保护敏感的真实数据。
 */
public class FeistelCipher {

    // ======================== 参数常量 ========================

    /** 分组大小：16 字节 */
    public static final int BLOCK_SIZE = 16;

    /** 左右半块大小：8 字节 */
    private static final int HALF_SIZE = 8;

    /** 轮数 */
    private static final int ROUNDS = 16;

    /** 子密钥字节数 */
    private static final int KEY_SIZE = 8;

    // ======================== S 盒 (仿射变换) ========================

    /**
     * 简单的可逆 S 盒：b → (b × 31 + 17) mod 256
     * 这是仿射变换，在模 256 下可逆（因为 31 与 256 互质）
     */
    private static final int[] SBOX = new int[256];
    private static final int[] SBOX_INV = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            SBOX[i] = (i * 31 + 17) % 256;
        }
        for (int i = 0; i < 256; i++) {
            // 求逆：找到 x 使得 SBOX[x] == i
            for (int j = 0; j < 256; j++) {
                if (SBOX[j] == i) {
                    SBOX_INV[i] = j;
                    break;
                }
            }
        }
    }

    // ======================== 公开接口 ========================

    /**
     * 加密数据
     * @param data     原始字节
     * @param password 密码
     * @return 加密后的字节（包含盐值 + 密文）
     */
    public static byte[] encrypt(byte[] data, String password) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        // 1. 生成盐值，防止相同数据产生相同密文
        byte[] salt = generateSalt(8);
        // 2. 从密码派生子密钥
        byte[][] roundKeys = deriveKeys(password, salt);
        // 3. PKCS7 填充
        byte[] padded = pad(data);
        // 4. 逐分组加密
        byte[] encrypted = new byte[padded.length];
        for (int i = 0; i < padded.length; i += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(padded, i, i + BLOCK_SIZE);
            byte[] processed = feistel(block, roundKeys, true);
            System.arraycopy(processed, 0, encrypted, i, BLOCK_SIZE);
        }
        // 5. 组装输出: salt(8) + 密文
        ByteBuffer buf = ByteBuffer.allocate(8 + encrypted.length);
        buf.put(salt);
        buf.put(encrypted);
        return buf.array();
    }

    /**
     * 解密数据
     * @param data     加密后的字节
     * @param password 密码
     * @return 解密后的原始字节
     */
    public static byte[] decrypt(byte[] data, String password) {
        if (data == null || data.length < 8 + BLOCK_SIZE) {
            return new byte[0];
        }
        // 1. 拆包：提取盐值
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte[] salt = new byte[8];
        buf.get(salt);
        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);
        // 2. 派生子密钥（和加密用相同的盐值）
        byte[][] roundKeys = deriveKeys(password, salt);
        // 3. 逐分组解密
        byte[] decrypted = new byte[ciphertext.length];
        for (int i = 0; i < ciphertext.length; i += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(ciphertext, i, i + BLOCK_SIZE);
            byte[] processed = feistel(block, roundKeys, false);
            System.arraycopy(processed, 0, decrypted, i, BLOCK_SIZE);
        }
        // 4. 去除 PKCS7 填充
        return unpad(decrypted);
    }

    // ======================== Feistel 核心 ========================

    /**
     * 对 16 字节分组执行 Feistel 网络加密/解密
     * @param block     16 字节输入
     * @param roundKeys 16 轮子密钥
     * @param encrypt   true=加密(密钥正序), false=解密(密钥逆序)
     * @return 16 字节输出
     */
    private static byte[] feistel(byte[] block, byte[][] roundKeys, boolean encrypt) {
        byte[] L = Arrays.copyOfRange(block, 0, HALF_SIZE);
        byte[] R = Arrays.copyOfRange(block, HALF_SIZE, BLOCK_SIZE);

        for (int round = 0; round < ROUNDS; round++) {
            // 解密时子密钥逆序使用
            int keyIndex = encrypt ? round : (ROUNDS - 1 - round);
            byte[] fOutput = roundFunction(R, roundKeys[keyIndex]);

            byte[] newL = R;
            byte[] newR = xor(L, fOutput);
            L = newL;
            R = newR;
        }

        // 最后一轮后 L 和 R 再交换一次（Feistel 标准做法，使加解密对称）
        byte[] result = new byte[BLOCK_SIZE];
        System.arraycopy(R, 0, result, 0, HALF_SIZE);       // R 在前
        System.arraycopy(L, 0, result, HALF_SIZE, HALF_SIZE); // L 在后
        return result;
    }

    // ======================== 轮函数 F ========================

    /**
     * 轮函数 F(R, subKey):
     *   1. 将 R 与子密钥异或
     *   2. 对每个字节应用 S 盒替换
     *
     * 你也可以在这里替换为更复杂的操作，例如：
     *   - 增加 P 盒（字节位置置换）
     *   - 使用更复杂的 S 盒
     *   - 增加扩散层（如 MDS 矩阵）
     */
    private static byte[] roundFunction(byte[] R, byte[] subKey) {
        byte[] output = new byte[HALF_SIZE];
        // Step 1: XOR with subkey
        byte[] xored = xor(R, subKey);
        // Step 2: SubBytes (S-box)
        for (int i = 0; i < HALF_SIZE; i++) {
            output[i] = (byte) SBOX[xored[i] & 0xFF];
        }
        return output;
    }

    // ======================== 密钥派生 ========================

    /**
     * 从密码派生 16 轮子密钥（每轮 8 字节）
     * 方法：用 SHA-256 对 (密码 + 盐值 + 轮次) 做哈希，取前 8 字节
     */
    private static byte[][] deriveKeys(String password, byte[] salt) {
        byte[][] roundKeys = new byte[ROUNDS][KEY_SIZE];
        byte[] pwdBytes = password.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < ROUNDS; i++) {
                md.update(pwdBytes);
                md.update(salt);
                md.update((byte) i);
                byte[] hash = md.digest();
                System.arraycopy(hash, 0, roundKeys[i], 0, KEY_SIZE);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
        return roundKeys;
    }

    // ======================== PKCS7 填充 ========================

    /**
     * PKCS7 填充：填充值为 "需要填充的字节数"
     * 即使数据已经是 16 字节对齐，也填充完整 16 字节
     */
    private static byte[] pad(byte[] data) {
        int padLen = BLOCK_SIZE - (data.length % BLOCK_SIZE);
        byte[] padded = new byte[data.length + padLen];
        System.arraycopy(data, 0, padded, 0, data.length);
        Arrays.fill(padded, data.length, padded.length, (byte) padLen);
        return padded;
    }

    /**
     * PKCS7 去填充：读取最后一个字节的值，移除相应数量的字节
     */
    private static byte[] unpad(byte[] data) {
        if (data.length == 0) return data;
        int padLen = data[data.length - 1] & 0xFF;
        if (padLen <= 0 || padLen > BLOCK_SIZE) {
            throw new RuntimeException("填充无效，密码错误或数据已损坏");
        }
        return Arrays.copyOf(data, data.length - padLen);
    }

    // ======================== 工具方法 ========================

    /** 生成随机盐值 */
    private static byte[] generateSalt(int length) {
        byte[] salt = new byte[length];
        // 用系统纳秒时间 + hashCode 做种子，模拟随机（非密码学安全，但可用于学习）
        long seed = System.nanoTime() ^ (long) (Math.random() * Long.MAX_VALUE);
        java.util.Random rng = new java.util.Random(seed);
        rng.nextBytes(salt);
        return salt;
    }

    /** 字节数组按位异或（要求长度相等） */
    private static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    // ======================== 演示入口 ========================

    public static void main(String[] args) {
        // 快速验证加解密正确性
        String password = "MySecretKey123!";
        String plaintext = "Hello, Feistel Cipher! This is a test for block cipher encryption.";

        System.out.println("原始文本: " + plaintext);
        System.out.println("原始字节: " + plaintext.getBytes().length + " 字节");

        byte[] encrypted = encrypt(plaintext.getBytes(StandardCharsets.UTF_8), password);
        System.out.println("加密后: " + encrypted.length + " 字节 (含 8 字节盐值)");
        System.out.println("加密 HEX: " + bytesToHex(encrypted));

        byte[] decrypted = decrypt(encrypted, password);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        System.out.println("解密结果: " + result);
        System.out.println("一致性: " + plaintext.equals(result));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
