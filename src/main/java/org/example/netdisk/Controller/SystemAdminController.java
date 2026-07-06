package org.example.netdisk.Controller;

import org.example.netdisk.Entity.Log;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.SystemAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class SystemAdminController {

    @Autowired
    private SystemAdminService systemAdminService;

    @PostMapping("/systemAdmin/updateUserInfo")
    public Result<String> updateUserInfo(
            @RequestParam("userId") Long userId,
            @RequestParam("newUsername") String newUsername,
            @RequestParam(value = "newGender", required = false) String newGender,
            @RequestPart(name = "newAvatar", required = false) MultipartFile newAvatar) {
        try {
            boolean ok = systemAdminService.updateUserInfo(userId, newUsername, newGender, newAvatar);
            if (ok) {
                return Result.success("更新用户信息成功");
            }
            return Result.error("更新用户信息失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/systemAdmin/resetPassword")
    public Result<String> resetPassword(@RequestParam("userId") Long userId) {
        boolean ok = systemAdminService.resetPassword(userId);
        if (ok) {
            return Result.success("重置密码成功");
        }
        return Result.error("重置密码失败");
    }

    @GetMapping("/systemAdmin/logs")
    public Result<List<Log>> getLogs() {
        List<Log> logs = systemAdminService.getLogs();
        return Result.success(logs);
    }
}
