package org.example.netdisk.Controller;

import org.example.netdisk.Entity.Log;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.SystemAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController // 系统管理员控制器，提供用户管理和日志查看等管理功能
public class SystemAdminController {

    @Autowired // 注入系统管理员服务层
    private SystemAdminService systemAdminService;

    @PostMapping("/systemAdmin/updateUserInfo") // 管理员更新任意用户的信息（用户名、性别、头像）
    public Result<String> updateUserInfo(
            @RequestParam("userId") Long userId, // 目标用户ID
            @RequestParam("newUsername") String newUsername, // 新的用户名
            @RequestParam(value = "newGender", required = false) String newGender, // 可选：新性别
            @RequestPart(name = "newAvatar", required = false) MultipartFile newAvatar) { // 可选：新头像文件，使用@RequestPart接收multipart数据
        try {
            boolean ok = systemAdminService.updateUserInfo(userId, newUsername, newGender, newAvatar); // 调用服务层更新用户信息
            if (ok) {
                return Result.success("更新用户信息成功"); // 更新成功
            }
            return Result.error("更新用户信息失败"); // 更新失败（如用户名重复或用户不存在）
        } catch (RuntimeException e) {
            return Result.error(e.getMessage()); // 捕获服务层抛出的运行时异常，将异常消息作为错误提示返回
        }
    }

    @PostMapping("/systemAdmin/resetPassword") // 管理员重置指定用户的密码为默认密码
    public Result<String> resetPassword(@RequestParam("userId") Long userId) { // 目标用户ID
        boolean ok = systemAdminService.resetPassword(userId); // 将该用户密码重置为系统默认密码
        if (ok) {
            return Result.success("重置密码成功"); // 重置成功
        }
        return Result.error("重置密码失败"); // 重置失败（如用户不存在）
    }

    @GetMapping("/systemAdmin/logs") // 获取系统操作日志，用于审计和追溯
    public Result<List<Log>> getLogs() {
        List<Log> logs = systemAdminService.getLogs(); // 查询所有系统操作日志记录
        return Result.success(logs); // 返回日志列表
    }
}
