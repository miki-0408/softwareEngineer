package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.RecycleBinService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 回收站控制器，管理用户已删除但未永久清除的文件
public class RecycleBinController {

    @Autowired // 注入回收站服务层
    private RecycleBinService recycleBinService;

    @PostMapping("/user/recycle/list") // 获取当前用户回收站中的所有文件列表
    public Result<List<R_File>> listRecycleFiles(
            @RequestHeader("Authorization") String authHeader) throws Exception { // JWT令牌验证用户身份
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 从令牌中提取用户ID
        List<R_File> list = recycleBinService.listRecycleFiles(userId); // 查询该用户回收站内的所有文件
        return Result.success(list); // 返回回收站文件列表
    }

    @PostMapping("/user/recycle/restore") // 从回收站恢复文件到原始位置
    public Result<String> restoreFile(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("fileId") Long fileId) throws Exception { // 要恢复的文件ID
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID，确保只能操作自己的回收站
        boolean ok = recycleBinService.restoreFile(userId, fileId); // 将文件从回收站恢复到删除前所在的目录
        if (ok) {
            return Result.success("文件恢复成功"); // 恢复成功
        }
        return Result.error("文件恢复失败"); // 恢复失败（如原目录已不存在）
    }

    @PostMapping("/user/recycle/deletePermanent") // 永久删除文件，不可恢复
    public Result<String> deletePermanently(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("fileId") Long fileId) throws Exception { // 要永久删除的文件ID
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID进行权限校验
        boolean ok = recycleBinService.deletePermanently(userId, fileId); // 从回收站中彻底删除文件，包括物理文件和数据记录
        if (ok) {
            return Result.success("文件已彻底删除"); // 永久删除成功
        }
        return Result.error("彻底删除失败"); // 永久删除失败
    }
}
