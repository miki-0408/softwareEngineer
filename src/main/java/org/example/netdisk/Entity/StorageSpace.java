package org.example.netdisk.Entity;

import lombok.Data;

@Data
public class StorageSpace {
    private Long userId;
    private Long totalSpace;
    private Long usedSpace;
    private Long remainSpace;
}
