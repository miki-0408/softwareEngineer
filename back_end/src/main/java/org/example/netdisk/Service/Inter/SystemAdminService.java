package org.example.netdisk.Service.Inter;

import org.example.netdisk.Entity.Log;

import java.util.List;

public interface SystemAdminService { // 系统管理员服务接口：管理员对用户的管理操作

    boolean updateUserInfo(Long userId, String newName, String newSex, org.springframework.web.multipart.MultipartFile newAvatar); // 管理员修改用户信息

    boolean resetPassword(Long userId); // 管理员重置用户密码为默认值

    List<Log> getLogs(); // 获取系统操作日志列表
}
