package org.example.netdisk.ResponseDTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class R_Directory {
    private String dirId;
    private String dirName;
    private String parentDirId;
    private LocalDateTime createTime;
}
