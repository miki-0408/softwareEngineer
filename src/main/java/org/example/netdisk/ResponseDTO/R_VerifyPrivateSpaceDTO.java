package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data
public class R_VerifyPrivateSpaceDTO {
    private Boolean verified;
    private String tempToken;
}
