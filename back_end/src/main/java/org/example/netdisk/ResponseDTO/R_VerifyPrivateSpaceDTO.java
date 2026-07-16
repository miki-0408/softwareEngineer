package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class R_VerifyPrivateSpaceDTO { // 私密空间验证响应DTO：验证结果和临时访问令牌
    private Boolean verified; // 验证是否通过
    private String tempToken; // 临时令牌，验证通过后用于访问私密空间文件
}
