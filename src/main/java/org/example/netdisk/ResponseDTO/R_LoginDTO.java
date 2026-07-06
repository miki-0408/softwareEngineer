package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data
public class R_LoginDTO {
    private R_User user;
    private String token;
}
