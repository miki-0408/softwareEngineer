package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_PrivateSpace;
import org.example.netdisk.ResponseDTO.R_VerifyPrivateSpaceDTO;

public interface PrivateSpaceService { // 私密空间服务接口：管理用户的加密私密空间

    boolean enablePrivateSpace(Long userId, String password); // 启用私密空间，设置访问密码

    boolean disablePrivateSpace(Long userId, String password); // 禁用私密空间，需验证密码

    R_VerifyPrivateSpaceDTO verifyPrivateSpace(Long userId, String password); // 验证私密空间密码，返回临时代币

    R_PrivateSpace getPrivateSpaceStatus(Long userId); // 查询私密空间启用状态
}
