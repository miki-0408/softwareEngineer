package org.example.netdisk.Entity;

import lombok.Data;


@Data
public class User {
    private String userId;             // 用户ID
    private String username;     // 用户名
    private String password;        // 密码
    private Integer sex;       // 性别
    private String avatar; // 头像路径
    private Integer role;          // 角色
}
