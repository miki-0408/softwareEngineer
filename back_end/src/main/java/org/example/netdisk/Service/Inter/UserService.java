package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_UserInfoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService { // 用户服务接口：定义用户注册、登录、信息管理等操作

    boolean register(String name, String password, String sex, MultipartFile avatar); // 注册新用户

    Map<String, Object> login(String name, String password); // 用户登录，返回包含JWT令牌的Map

    R_UserInfoDTO getUserInfo(Long requesterId, Long userId); // 获取用户信息（含存储空间和私密空间状态）

    boolean changePassword(Long userId, String oldPassword, String newPassword); // 修改密码

    boolean updateUserInfo(Long userId, String newName, String newSex, MultipartFile newAvatar); // 更新用户个人信息
}
