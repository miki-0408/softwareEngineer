package org.example.netdisk.ResponseDTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data // Lombok自动生成getter/setter等
public class R_Directory { // 目录响应DTO：返回给前端的目录信息
    private String dirId; // 目录ID（字符串格式，方便前端处理）
    private String dirName; // 目录名称
    private String parentDirId; // 父目录ID，null表示根目录
    private LocalDateTime createTime; // 创建时间
}
