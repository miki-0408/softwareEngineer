package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.*;
import org.example.netdisk.Service.Inter.UserService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController // 用户控制器，处理注册、登录、个人信息管理等用户相关操作
public class UserController {

    @Autowired // 注入用户服务层
    private UserService userService;

    @PostMapping("/register") // 用户注册API，无需JWT令牌即可访问
    public Result<String> register(
            @RequestParam("username") String username, // 注册用户名
            @RequestParam("password") String password, // 注册密码
            @RequestParam(value = "gender", required = false) String gender, // 可选：用户性别
            @RequestPart(name = "avatar", required = false) MultipartFile avatar) { // 可选：用户头像文件
        boolean success = userService.register(username, password, gender, avatar); // 调用服务层注册新用户，包括头像上传和密码加密
        return success ? Result.success("注册成功") : Result.error("注册失败，用户名已存在"); // 返回注册结果，用户名重复时提示失败
    }

    @PostMapping("/login") // 用户登录API，验证用户名密码并返回JWT令牌
    public Result<R_LoginDTO> login(
            @RequestParam("username") String username, // 登录用户名
            @RequestParam("password") String password) { // 登录密码
        Map<String, Object> response = userService.login(username, password); // 调用登录服务，返回包含登录DTO或错误消息的Map
        if (!"null".equals(response.get("rLoginDTO"))) { // 判断登录是否成功：成功时rLoginDTO不为null
            return Result.success((R_LoginDTO) response.get("rLoginDTO")); // 返回登录成功信息，包含用户信息、JWT令牌和私密空间状态
        } else {
            return Result.error((String) response.get("message")); // 登录失败，返回错误原因（用户名不存在或密码错误）
        }
    }

    @PostMapping("/userInfo") // 获取用户公开信息API，支持查看自己或他人的基本信息
    public Result<R_UserInfoDTO> getUserInfo(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证请求者身份
            @RequestParam("userId") Long userId) throws Exception { // 目标查询用户ID
        String requesterIdStr = (String) TokenProcess.getAttributeFromToken(authHeader, "userId"); // 从令牌中提取请求者的用户ID
        Long requesterId = Long.parseLong(requesterIdStr); // 将字符串ID转为长整型
        R_UserInfoDTO data = userService.getUserInfo(requesterId, userId); // 查询用户信息，请求者ID用于权限判断
        if (data == null) {
            return Result.error("用户不存在"); // 目标用户不存在
        }
        return Result.success(data); // 返回用户信息（用户名、性别、头像等）
    }

    @PostMapping("/changePassword") // 修改当前登录用户的密码
    public Result<String> changePassword(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("oldPassword") String oldPassword, // 旧密码，用于验证操作者身份
            @RequestParam("newPassword") String newPassword) throws Exception { // 新密码
        String userIdStr = (String) TokenProcess.getAttributeFromToken(authHeader, "userId"); // 从令牌中提取用户ID
        Long userId = Long.parseLong(userIdStr); // 转换用户ID类型
        boolean ok = userService.changePassword(userId, oldPassword, newPassword); // 验证旧密码后更新为新密码
        if (!ok) {
            return Result.error("密码修改失败，旧密码错误"); // 旧密码验证失败，拒绝修改
        }
        return Result.success("密码修改成功"); // 密码修改成功
    }

    @PostMapping("/user/updateUserInfo") // 当前登录用户更新自己的个人信息
    public Result<String> updateUserInfo(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("newUsername") String newUsername, // 新用户名
            @RequestParam(value = "newGender", required = false) String newGender, // 可选：新性别
            @RequestPart(name = "newAvatar", required = false) MultipartFile newAvatar) throws Exception { // 可选：新头像文件
        String userIdStr = (String) TokenProcess.getAttributeFromToken(authHeader, "userId"); // 从令牌中提取当前用户ID
        Long userId = Long.parseLong(userIdStr); // 转换用户ID类型
        boolean ok = userService.updateUserInfo(userId, newUsername, newGender, newAvatar); // 更新用户信息，包括文件上传
        return ok ? Result.success("更新成功") : Result.error("更新失败"); // 返回更新结果
    }
}
