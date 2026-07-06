package org.example.netdisk.Utils;

/**
 * 加密 / 解密工具
 *
 * ██████  当前实现已清空  ██████
 * 请在下方 TODO 处填入你选择的加密算法。
 *
 * 建议手动实现的算法：
 * ┌──────────────────────────────────────────────────────────────┐
 * │ ① Feistel 网络密码 (FeistelCipher.java)                      │
 * │    - 分组密码，结构优雅，加解密共用同一套代码（子密钥逆序）     │
 * │    - 可自选轮函数 F，灵活可扩展                                │
 * │    - 参考完整实现见 FeistelCipher.java                        │
 * ├──────────────────────────────────────────────────────────────┤
 * │ ② XOR 流密码                                                │
 * │    - 最简单的流密码，从密码派生密钥流与数据异或                │
 * │    - 注意：密钥流不能重复使用，否则可被两次异或破解            │
 * │    - 适合学习但不适合生产                                      │
 * └──────────────────────────────────────────────────────────────┘
 *
 * 标准参考实现见 StandardEncryption.java（AES-256-GCM + PBKDF2）
 */
public class EncryptionUtil {

    /**
     * 加密数据
     * @param data     原始字节
     * @param password 密码
     * @return 加密后的字节（应包含解密所需的所有信息，如盐值、IV 等）
     */
    public static byte[] encrypt(byte[] data, String password) {
        // TODO: 在这里实现你的手动加密算法
        // 可参考 FeistelCipher.encrypt() 的实现
        throw new UnsupportedOperationException("未实现手动加密算法");
    }

    /**
     * 解密数据
     * @param data     加密后的字节
     * @param password 密码
     * @return 解密后的原始字节
     */
    public static byte[] decrypt(byte[] data, String password) {
        // TODO: 在这里实现你的手动解密算法
        // 可参考 FeistelCipher.decrypt() 的实现
        throw new UnsupportedOperationException("未实现手动解密算法");
    }
}
