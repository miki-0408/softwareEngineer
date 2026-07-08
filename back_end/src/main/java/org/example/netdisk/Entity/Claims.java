package org.example.netdisk.Entity;

import java.util.Map;

public record Claims(String userId, String role) {
    public Map<String, Object> toMap() {
        return Map.of("userId", userId, "role", role);
    }

    public static Claims fromMap(Map<String, ?> m) {
        if (m == null) return null;
        return new Claims((String) m.get("userId"), (String) m.get("role"));
    }
}
