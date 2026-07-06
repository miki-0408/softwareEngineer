package org.example.netdisk.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Directory {
    private Long dirId;
    private String dirName;
    private Long parentDirId;
    private Long userId;
    private LocalDateTime createTime;
}
