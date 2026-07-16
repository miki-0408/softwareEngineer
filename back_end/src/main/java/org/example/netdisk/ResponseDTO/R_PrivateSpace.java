package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class R_PrivateSpace { // 私密空间响应DTO：返回私密空间状态
    private Boolean enabled; // 是否已启用私密空间
    private String rootDirId; // 私密空间根目录ID
}
