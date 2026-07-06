package org.example.netdisk.Utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 基于 BCrypt 的密码工具类。
 * 用法：
 * - 生成哈希：String hash = BcryptUtil.hash(raw);
 * - 校验：boolean ok = BcryptUtil.matches(raw, hash);
 */
public final class BcryptUtil {
    // 默认强度 10，可按需调整到 12~14。强度越高计算越慢越安全。
    private static final int DEFAULT_STRENGTH = 12;

    // PasswordEncoder 线程安全，可全局复用。
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(DEFAULT_STRENGTH);

    private BcryptUtil() {}

    /**
     * 生成 BCrypt 哈希（包含随机盐与成本因子）。
     */
    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验明文与哈希是否匹配。
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, encodedPassword);
    }

}

