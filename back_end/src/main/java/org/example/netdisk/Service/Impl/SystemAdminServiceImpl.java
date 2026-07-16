package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.Log;
import org.example.netdisk.Entity.User;
import org.example.netdisk.Mapper.LogMapper;
import org.example.netdisk.Mapper.UserMapper;
import org.example.netdisk.Service.Inter.SystemAdminService;
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
    private UserMapper usermapper; // 用户表数据库操作接口
    @Autowired
    private LogMapper logmapper; // 日志表数据库操作接口
    @Autowired
    private UserServiceImpl userService; // 用户服务实现，复用其更新用户信息的方法

    @Override
    public boolean updateUserInfo(Long userId, String newName, String newSex, MultipartFile newAvatar) { // 管理员更新任意用户信息，委托给UserService执行
        return userService.updateUserInfo(userId, newName, newSex, newAvatar); // 直接复用UserServiceImpl的更新逻辑
    }

    @Override
    public boolean resetPassword(Long userId) { // 管理员重置任意用户密码为系统默认密码
        User user = usermapper.selectUserById(userId); // 根据userId查询用户记录
        if (user != null) { // 用户存在才执行重置
            user.setPassword(BcryptUtil.hash(defaultPWD)); // 使用BCrypt加密默认密码后设置到用户实体
            usermapper.updateUser(user); // 将重置后的密码写入数据库
            return true; // 重置成功
        }
        return false; // 用户不存在，重置失败
    }

    @Override
    public List<Log> getLogs() { // 管理员查看全量操作日志
        return logmapper.selectAllLogs(); // 查询并返回所有日志记录
    }
}
