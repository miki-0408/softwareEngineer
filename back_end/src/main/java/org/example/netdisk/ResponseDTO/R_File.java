package org.example.netdisk.ResponseDTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class R_File {
    private String fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadTime;
    private String dirId;
    private String originalDirId;
    private Integer status;
    private Integer isEncrypted;
    private Integer compressMethod;
}
