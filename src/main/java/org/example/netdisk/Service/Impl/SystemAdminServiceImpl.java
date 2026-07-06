package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.PCOI.Entity.Log;
import org.example.PCOI.Entity.User;
import org.example.PCOI.Mapper.LogMapper;
import org.example.PCOI.Mapper.UserMapper;
import org.example.PCOI.Service.Inter.SystemAdminService;
import org.example.PCOI.Service.Support.FileStorageService;
import org.example.PCOI.Utils.BcryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.example.PCOI.Service.Support.Enum.*;
@Slf4j
@Service
public class SystemAdminServiceImpl implements SystemAdminService {
    @Autowired
    private UserMapper usermapper;
    @Autowired
    private LogMapper logmapper;
    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public boolean updateUserInfo(String userId, String newUsername, Integer newGender, MultipartFile newAvatar) {
        try {
            // 1) 按 userId 查询目标用户
            User user = usermapper.selectUserById(userId);
            if (user == null) {
                return false; // 用户不存在
            }
            // 2) 若提供了新用户名，校验是否被其他用户占用
            if (newUsername != null && !newUsername.isEmpty()) {
                User existingUser = usermapper.selectUserByName(newUsername);
                if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                    return false; // 新用户名已被其他用户使用
                }
                user.setUsername(newUsername); // 覆盖用户名
            }
            // 3) 若提供了新性别，直接覆盖
            if (newGender != null) {
                user.setSex(newGender);
            }
            // 4) 若上传了新头像，保存文件并更新头像URL
            if (newAvatar != null && !newAvatar.isEmpty()) {
                String avatarUrl = fileStorageService.saveAvatar(newAvatar, userId);
                user.setAvatar(avatarUrl);
            }
            // 5) 持久化更新
            usermapper.updateUser(user);
            return true;
        }catch(RuntimeException e){
            throw new RuntimeException("更新用户信息失败:"+e.getMessage());
        }
    }

    @Override
    public boolean resetPassword(String userId) {
        User user = usermapper.selectUserById(userId);
        if (user != null) {
            String defaultPassword = BcryptUtil.hash(defaultPWD);
            user.setPassword(defaultPassword);
            usermapper.updateUser(user);
            return true;
        }
        return false;
    }

    @Override
    public List<Log> getLogs() {
        return logmapper.selectAllLogs();
    }
}
