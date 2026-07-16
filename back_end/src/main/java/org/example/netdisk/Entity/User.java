package org.example.netdisk.Entity;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class User {
    private Long userId; // 用户唯一ID（自增主键）
    private String name; // 用户名（登录名）
    private String password; // 密码（BCrypt哈希存储）
    private String sex; // 性别
    private String avatar; // 头像URL
    private String role; // 角色：user=普通用户，admin=系统管理员
}
