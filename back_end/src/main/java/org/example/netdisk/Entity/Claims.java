package org.example.netdisk.Entity;

import java.util.Map;

public record Claims(String userId, String role) { // JWT载荷数据记录：封装用户ID和角色信息
    public Map<String, Object> toMap() { // 将Claims转为Map，用于生成JWT令牌
        return Map.of("userId", userId, "role", role); // 构建不可变Map
    }

    public static Claims fromMap(Map<String, ?> m) { // 从Map还原Claims对象，用于解析JWT令牌
        if (m == null) return null; // 空Map返回null
        return new Claims((String) m.get("userId"), (String) m.get("role")); // 从Map提取userId和role构造Claims
    }
}
