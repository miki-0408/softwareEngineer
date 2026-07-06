package org.example.netdisk.Entity;

import lombok.Data;

@Data
public class PrivateSpace {
    private Long userId;
    private String password;
    private Integer isEncrypted;
}
