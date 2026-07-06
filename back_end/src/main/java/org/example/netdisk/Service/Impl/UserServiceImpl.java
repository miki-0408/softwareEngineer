package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.*;
import org.example.netdisk.Mapper.*;
import org.example.netdisk.ResponseDTO.*;
import org.example.netdisk.Service.Inter.UserService;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Service.Support.TransformService;
import org.example.netdisk.Utils.BcryptUtil;
import org.example.netdisk.Utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.example.netdisk.Service.Support.Enum.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper usermapper;
    @Autowired
    private StorageSpaceMapper storageSpaceMapper;
    @Autowired
    private DirectoryMapper directoryMapper;
    @Autowired
    private PrivateSpaceMapper privateSpaceMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private TransformService transformService;

    @Override
    public boolean register(String name, String password, String sex, MultipartFile avatar) {
        if (usermapper.selectUserByName(name) != null) {
            return false;
        }
        User user = new User();
        user.setName(name);
        user.setPassword(BcryptUtil.hash(password));
        user.setSex(sex == null || sex.isBlank() ? unknown : sex);
        user.setRole(normalUser);
        usermapper.insertUser(user);

        StorageSpace storageSpace = new StorageSpace();
        storageSpace.setUserId(user.getUserId());
        storageSpace.setTotalSpace(defaultTotalSpace);
        storageSpace.setUsedSpace(0L);
        storageSpace.setRemainSpace(defaultTotalSpace);
        storageSpaceMapper.insertStorageSpace(storageSpace);

        Directory rootDir = new Directory();
        rootDir.setDirName(rootDirName);
        rootDir.setParentDirId(null);
        rootDir.setUserId(user.getUserId());
        directoryMapper.insertDirectory(rootDir);

        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = fileStorageService.saveAvatar(avatar, user.getUserId());
            user.setAvatar(avatarUrl);
            usermapper.updateUser(user);
        }
        return true;
    }

    @Override
    public Map<String, Object> login(String name, String password) {
        User user = usermapper.selectUserByName(name);
        if (user == null) {
            return Map.of("rLoginDTO", "null", "message", "用户不存在");
        }
        if (!BcryptUtil.matches(password, user.getPassword())) {
            return Map.of("rLoginDTO", "null", "message", "密码错误");
        }
        Claims claims = new Claims(user.getName(), String.valueOf(user.getUserId()), user.getRole(), login);
        String token = JwtUtil.genToken(claims.toMap());
        R_User rUser = transformService.transformUserToRUser(user);
        R_LoginDTO loginDTO = new R_LoginDTO();
        loginDTO.setUser(rUser);
        loginDTO.setToken(token);
        return Map.of("rLoginDTO", loginDTO, "message", "登录成功");
    }

    @Override
    public R_UserInfoDTO getUserInfo(Long requesterId, Long userId) {
        User user = usermapper.selectUserById(userId);
        if (user == null) {
            return null;
        }
        R_UserInfoDTO dto = new R_UserInfoDTO();
        dto.setUser(transformService.transformUserToRUser(user));
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
        if (storageSpace != null) {
            dto.setStorage(transformService.transformStorageSpaceToRStorageSpace(storageSpace));
        }
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId);
        dto.setPrivateSpace(transformService.transformPrivateSpaceToRPrivateSpace(privateSpace));
        return dto;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = usermapper.selectUserById(userId);
        if (user == null) {
            return false;
        }
        if (!BcryptUtil.matches(oldPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(BcryptUtil.hash(newPassword));
        usermapper.updateUser(user);
        return true;
    }

    @Override
    public boolean updateUserInfo(Long userId, String newName, String newSex, MultipartFile newAvatar) {
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
    }
}
