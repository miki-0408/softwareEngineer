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
    private UserMapper usermapper; // 用户表数据库操作接口
    @Autowired
    private StorageSpaceMapper storageSpaceMapper; // 存储空间表数据库操作接口
    @Autowired
    private DirectoryMapper directoryMapper; // 目录表数据库操作接口，用于创建用户根目录
    @Autowired
    private PrivateSpaceMapper privateSpaceMapper; // 私密空间表数据库操作接口，用于查询状态
    @Autowired
    private FileStorageService fileStorageService; // 物理文件存储服务，用于保存头像文件
    @Autowired
    private TransformService transformService; // 实体与响应DTO之间的转换服务

    @Override
    public boolean register(String name, String password, String sex, MultipartFile avatar) { // 用户注册流程：校验用户名→验证头像→插入用户→创建存储空间→创建根目录→保存头像
        if (usermapper.selectUserByName(name) != null) { // 查询用户名是否已被占用
            return false; // 用户名已存在，注册失败
        }
        if (avatar != null && !avatar.isEmpty()) { // 头像文件不为空，需要校验文件类型
            String contentType = avatar.getContentType(); // 获取上传文件的MIME类型
            if (contentType != null && !contentType.startsWith("image/")) { // MIME类型不以image/开头，说明不是图片
                throw new RuntimeException("仅支持图片类型文件"); // 拒绝非图片头像，防止恶意文件上传
            }
        }
        User user = new User(); // 创建用户实体对象
        user.setName(name); // 设置用户名
        user.setPassword(BcryptUtil.hash(password)); // 使用BCrypt加密密码后存储，不存明文
        user.setSex(sex == null || sex.isBlank() ? unknown : sex); // 性别为空或空白则设为未知
        user.setRole(normalUser); // 新注册用户默认角色为普通用户
        usermapper.insertUser(user); // 插入用户记录到数据库，获取自增主键userId

        StorageSpace storageSpace = new StorageSpace(); // 创建存储空间实体
        storageSpace.setUserId(user.getUserId()); // 绑定用户ID
        storageSpace.setTotalSpace(defaultTotalSpace); // 设置系统默认总存储容量
        storageSpace.setUsedSpace(0L); // 新用户已用空间初始化为0
        storageSpace.setRemainSpace(defaultTotalSpace); // 剩余空间初始等于总空间
        storageSpaceMapper.insertStorageSpace(storageSpace); // 插入存储空间记录到数据库

        Directory rootDir = new Directory(); // 创建用户根目录实体
        rootDir.setDirName(rootDirName); // 设置根目录名称（取值自枚举常量）
        rootDir.setParentDirId(null); // 根目录的父目录ID为null
        rootDir.setUserId(user.getUserId()); // 绑定归属用户
        directoryMapper.insertDirectory(rootDir); // 插入根目录记录到数据库

        if (avatar != null && !avatar.isEmpty()) { // 头像文件不为空，保存到磁盘
            String avatarUrl = fileStorageService.saveAvatar(avatar, user.getUserId()); // 将头像保存到磁盘并获取访问URL
            user.setAvatar(avatarUrl); // 设置用户头像URL
            usermapper.updateUser(user); // 更新用户记录（补充头像URL）
        }
        return true; // 注册成功
    }

    @Override
    public Map<String, Object> login(String name, String password) { // 用户登录流程：查用户→验密码→生成token→组装返回信息
        User user = usermapper.selectUserByName(name); // 根据用户名查询用户记录
        if (user == null) { // 用户不存在
            return Map.of("rLoginDTO", "null", "message", "用户不存在"); // 返回错误信息
        }
        if (!BcryptUtil.matches(password, user.getPassword())) { // BCrypt密码校验不通过
            return Map.of("rLoginDTO", "null", "message", "密码错误"); // 返回错误信息
        }
        Claims claims = new Claims(String.valueOf(user.getUserId()), user.getRole()); // 构造JWT声明的Claims对象（含userId和角色）
        String token = JwtUtil.genToken(claims.toMap()); // 使用Claims生成JWT令牌
        R_User rUser = transformService.transformUserToRUser(user); // 将用户实体转为前端友好的响应DTO
        R_LoginDTO loginDTO = new R_LoginDTO(); // 创建登录响应DTO
        loginDTO.setUser(rUser); // 放入用户信息
        loginDTO.setToken(token); // 放入JWT令牌
        return Map.of("rLoginDTO", loginDTO, "message", "登录成功"); // 返回登录结果
    }

    @Override
    public R_UserInfoDTO getUserInfo(Long requesterId, Long userId) { // 获取用户完整信息：用户资料 + 存储空间 + 私密空间状态
        User user = usermapper.selectUserById(userId); // 根据userId查询用户记录
        if (user == null) { // 用户不存在
            return null; // 返回null表示未找到
        }
        R_UserInfoDTO dto = new R_UserInfoDTO(); // 创建用户信息响应DTO
        dto.setUser(transformService.transformUserToRUser(user)); // 转换并设置用户基本资料
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId); // 查询用户存储空间记录
        if (storageSpace != null) { // 存储记录存在才设置
            dto.setStorage(transformService.transformStorageSpaceToRStorageSpace(storageSpace)); // 转换并设置存储空间信息
        }
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId); // 查询用户私密空间记录
        dto.setPrivateSpace(transformService.transformPrivateSpaceToRPrivateSpace(privateSpace)); // 转换并设置私密空间状态
        return dto; // 返回完整的用户信息DTO
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) { // 修改密码：查用户→验旧密码→加密新密码→更新记录
        User user = usermapper.selectUserById(userId); // 根据userId查询用户记录
        if (user == null) { // 用户不存在
            return false; // 修改失败
        }
        if (!BcryptUtil.matches(oldPassword, user.getPassword())) { // 旧密码BCrypt校验不通过
            return false; // 旧密码错误，拒绝修改
        }
        user.setPassword(BcryptUtil.hash(newPassword)); // 使用BCrypt加密新密码后设置
        usermapper.updateUser(user); // 将新密码写入数据库
        return true; // 修改成功
    }

    @Override
    public boolean updateUserInfo(Long userId, String newName, String newSex, MultipartFile newAvatar) { // 更新用户信息：支持部分更新（名/性别/头像均可选）
        User user = usermapper.selectUserById(userId); // 根据userId查询用户记录
        if (user == null) { // 用户不存在
            return false; // 更新失败
        }
        if (newName != null && !newName.isEmpty()) { // 传入了新用户名
            User existingUser = usermapper.selectUserByName(newName); // 查询新用户名是否已被其他用户占用
            if (existingUser != null && !existingUser.getUserId().equals(userId)) { // 被其他用户占用
                return false; // 用户名冲突，更新失败
            }
            user.setName(newName); // 更新用户名
        }
        if (newSex != null && !newSex.isEmpty()) { // 传入了新性别
            user.setSex(newSex); // 更新性别字段
        }
        if (newAvatar != null && !newAvatar.isEmpty()) { // 传入了新头像文件
            String avatarUrl = fileStorageService.saveAvatar(newAvatar, userId); // 保存头像到磁盘并获取URL
            user.setAvatar(avatarUrl); // 更新头像URL字段
        }
        usermapper.updateUser(user); // 将所有变更写入数据库
        return true; // 更新成功
    }
}
