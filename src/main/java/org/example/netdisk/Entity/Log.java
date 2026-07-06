package org.example.netdisk.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Log {
    private Long logId;
    private Long operatorId;
    private LocalDateTime time;
    private String description;
}
