package org.example.netdisk.Entity;

import lombok.Data;

@Data
public class User {
    private Long userId;
    private String name;
    private String password;
    private String sex;
    private String avatar;
    private String role;
}
