package org.example.netdisk.ResponseDTO;

import lombok.Data;


@Data
public class R_User {
    private String userId;             // 用户ID
    private Integer role;          // 角色
    private Integer status;         // 状态
    private String username;     // 用户名
    private Integer sex;       // 性别
    private String avatar; // 头像路径
}
