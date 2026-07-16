package org.example.netdisk.Entity;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class PrivateSpace {
    private Long userId; // 用户ID（与user表关联）
    private String password; // 私密空间密码（BCrypt哈希存储）
    private Integer isEncrypted; // 私密空间启用状态：0=未启用，1=已启用
}
