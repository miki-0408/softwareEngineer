package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.Log;
import org.example.netdisk.Entity.User;
import org.example.netdisk.Mapper.LogMapper;
import org.example.netdisk.Mapper.UserMapper;
import org.example.netdisk.Service.Inter.SystemAdminService;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Utils.BcryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.example.netdisk.Service.Support.Enum.defaultPWD;

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
    public boolean updateUserInfo(Long userId, String newName, String newSex, MultipartFile newAvatar) {
        try {
            User user = usermapper.selectUserById(userId);
            if (user == null) {
                return false;
            }
            if (newName != null && !newName.isEmpty()) {
                User existingUser = usermapper.selectUserByName(newName);
                if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                    return false;
                }
                user.setName(newName);
            }
            if (newSex != null && !newSex.isEmpty()) {
                user.setSex(newSex);
            }
            if (newAvatar != null && !newAvatar.isEmpty()) {
                String avatarUrl = fileStorageService.saveAvatar(newAvatar, userId);
                user.setAvatar(avatarUrl);
            }
            usermapper.updateUser(user);
            return true;
        } catch (RuntimeException e) {
            throw new RuntimeException("更新用户信息失败:" + e.getMessage());
        }
    }

    @Override
    public boolean resetPassword(Long userId) {
        User user = usermapper.selectUserById(userId);
        if (user != null) {
            user.setPassword(BcryptUtil.hash(defaultPWD));
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
