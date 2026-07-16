package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class R_UserInfoDTO { // 用户综合信息响应DTO：聚合用户、存储、私密空间三类信息
    private R_User user; // 用户基本信息
    private R_StorageSpace storage; // 存储空间信息
    private R_PrivateSpace privateSpace; // 私密空间状态信息
}
