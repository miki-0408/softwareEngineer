package org.example.netdisk.Utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class BcryptUtil {
    private static final int DEFAULT_STRENGTH = 12; // BCrypt哈希强度，值越大越安全但计算越慢

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(DEFAULT_STRENGTH); // 全局复用编码器，线程安全

    private BcryptUtil() {} // 工具类禁止实例化

    public static String hash(String rawPassword) { // 对明文密码生成BCrypt哈希值
        if (rawPassword == null) { // 拒绝空密码
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        return ENCODER.encode(rawPassword); // 生成包含随机盐的哈希值
    }

    public static boolean matches(String rawPassword, String encodedPassword) { // 校验明文密码与哈希值是否匹配
        if (rawPassword == null || encodedPassword == null) { // 任一个为空则直接返回不匹配
            return false;
        }
        return ENCODER.matches(rawPassword, encodedPassword); // 比对明文与哈希
    }

}
