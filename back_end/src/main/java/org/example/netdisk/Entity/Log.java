package org.example.netdisk.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data // Lombok自动生成getter/setter等
public class Log {
    private Long logId; // 日志唯一ID（自增主键）
    private Long operatorId; // 操作者用户ID
    private LocalDateTime time; // 操作时间
    private String description; // 操作描述（如请求URI）
}
