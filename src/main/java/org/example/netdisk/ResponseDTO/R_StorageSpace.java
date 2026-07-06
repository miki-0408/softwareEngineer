package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data
public class R_StorageSpace {
    private Long totalSpace;
    private Long usedSpace;
    private Long remainSpace;
}
