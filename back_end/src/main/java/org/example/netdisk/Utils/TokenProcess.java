package org.example.netdisk.Utils;

import org.example.netdisk.Entity.Claims;

import java.util.Map;

public class TokenProcess {
    public static Object getAttributeFromToken(String authHeader, String key) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new Exception("Invalid Authorization header");
        }
        String token = authHeader.substring(7);
        Map<String, Object> map = JwtUtil.parseToken(token);
        Claims claims = Claims.fromMap(map);
        Object value = switch (key) {
            case "userId" -> claims.userId();
            case "role" -> claims.role();
            default -> throw new Exception("Invalid key: " + key);
        };
        if (value == null) {
            throw new Exception("Key not found in token claims: " + key);
        }
        return value;
    }
}
