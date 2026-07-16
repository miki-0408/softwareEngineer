package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class R_StorageSpace { // 存储空间响应DTO：返回用户的存储配额信息
    private Long totalSpace; // 总存储空间（字节）
    private Long usedSpace; // 已用空间（字节）
    private Long remainSpace; // 剩余空间（字节）
}
