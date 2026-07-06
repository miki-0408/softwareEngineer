package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.*;
import org.example.netdisk.Service.Inter.UserService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<String> register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestPart(name = "avatar", required = false) MultipartFile avatar) {
        try {
            boolean success = userService.register(username, password, gender, avatar);
            if (success) {
                return Result.success("注册成功");
            } else {
                return Result.error("注册失败，用户名已存在");
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<R_LoginDTO> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
        Map<String, Object> response = userService.login(username, password);
        if (!"null".equals(response.get("rLoginDTO"))) {
            return Result.success((R_LoginDTO) response.get("rLoginDTO"));
        } else {
            return Result.error((String) response.get("message"));
        }
    }

    @PostMapping("/userInfo")
    public Result<R_UserInfoDTO> getUserInfo(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("userId") Long userId) throws Exception {
        String requesterIdStr = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        Long requesterId = Long.parseLong(requesterIdStr);
        R_UserInfoDTO data = userService.getUserInfo(requesterId, userId);
        if (data == null) {
            return Result.error("用户不存在");
        }
        return Result.success(data);
    }

    @PostMapping("/changePassword")
    public Result<String> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword) throws Exception {
        String userIdStr = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        Long userId = Long.parseLong(userIdStr);
        boolean ok = userService.changePassword(userId, oldPassword, newPassword);
        if (!ok) {
            return Result.error("密码修改失败，旧密码错误");
        }
        return Result.success("密码修改成功");
    }

    @PostMapping("/user/updateUserInfo")
    public Result<String> updateUserInfo(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("newUsername") String newUsername,
            @RequestParam(value = "newGender", required = false) String newGender,
            @RequestPart(name = "newAvatar", required = false) MultipartFile newAvatar) throws Exception {
        try {
            String userIdStr = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
            Long userId = Long.parseLong(userIdStr);
            boolean ok = userService.updateUserInfo(userId, newUsername, newGender, newAvatar);
            if (ok) {
                return Result.success("更新成功");
            }
            return Result.error("更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
