package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data
public class R_VerifySecurityIssuesDTO {
    private Boolean verified; // 是否验证成功
    private String tempToken; // 临时令牌
}
