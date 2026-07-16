package org.example.netdisk.Entity;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class StorageSpace {
    private Long userId; // 用户ID
    private Long totalSpace; // 总存储空间（字节，默认10GB）
    private Long usedSpace; // 已用空间（字节）
    private Long remainSpace; // 剩余空间（字节）
}
