package org.example.netdisk.Entity;

import java.util.Map;

public record Claims(String username, String userId, String role, Integer type) {
    public Map<String, Object> toMap() {
        return Map.of(
                "username", username,
                "userId", userId,
                "role", role,
                "type", type
        );
    }

    public static Claims fromMap(Map<String, ?> m) {
        if (m == null) return null;
        String username = (String) m.get("username");
        String userId = (String) m.get("userId");
        String role = (String) m.get("role");
        Integer type = (Integer) m.get("type");
        return new Claims(username, userId, role, type);
    }
}
