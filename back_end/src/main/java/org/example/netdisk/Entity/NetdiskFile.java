package org.example.netdisk.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NetdiskFile {
    private Long fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String path;
    private LocalDateTime uploadTime;
    private Long userId;
    private Long dirId;
    private Integer status;
    private Integer isEncrypted;
}
