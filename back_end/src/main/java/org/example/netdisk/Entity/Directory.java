package org.example.netdisk.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data // Lombok自动生成getter/setter/toString/equals/hashCode
public class Directory {
    private Long dirId; // 目录唯一ID（自增主键）
    private String dirName; // 目录名称
    private Long parentDirId; // 父目录ID，为null表示根目录
    private Long userId; // 所属用户ID
    private LocalDateTime createTime; // 创建时间
}
