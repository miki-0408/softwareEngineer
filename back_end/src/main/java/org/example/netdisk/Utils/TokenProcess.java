package org.example.netdisk.Utils;

import org.example.netdisk.Entity.Claims;

import java.util.Map;

public class TokenProcess {
    public static Object getAttributeFromToken(String authHeader, String key) throws Exception { // 从Authorization头中解析JWT并提取指定属性值
        if (authHeader == null || !authHeader.startsWith("Bearer ")) { // 验证Authorization头格式
            throw new Exception("Invalid Authorization header");
        }
        String token = authHeader.substring(7); // 去掉"Bearer "前缀获取纯令牌
        Map<String, Object> map = JwtUtil.parseToken(token); // 解析JWT获取载荷Map
        Claims claims = Claims.fromMap(map); // 将Map转换为类型安全的Claims记录
        Object value = switch (key) { // 根据key返回对应属性（JDK 17+ switch表达式）
            case "userId" -> claims.userId(); // 返回用户ID
            case "role" -> claims.role(); // 返回用户角色
            default -> throw new Exception("Invalid key: " + key); // 不支持的属性键
        };
        if (value == null) { // 属性值为空时抛异常
            throw new Exception("Key not found in token claims: " + key);
        }
        return value; // 返回提取的属性值
    }
}
