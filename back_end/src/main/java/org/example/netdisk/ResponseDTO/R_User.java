package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class R_User { // 用户响应DTO：返回给前端的用户信息（不含密码）
    private String userId; // 用户ID（字符串格式）
    private String role; // 角色：user或admin
    private String name; // 用户名
    private String sex; // 性别
    private String avatar; // 头像URL
}
