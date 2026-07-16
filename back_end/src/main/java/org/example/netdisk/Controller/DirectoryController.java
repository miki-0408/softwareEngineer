package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.R_Directory;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.DirectoryService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 标记为REST控制器，所有方法返回值自动序列化为JSON
public class DirectoryController {

    @Autowired // 自动注入目录服务层实现
    private DirectoryService directoryService;

    @PostMapping("/user/directory/create") // 创建目录的API端点
    public Result<R_Directory> createDirectory(
            @RequestHeader("Authorization") String authHeader, // 从请求头获取JWT令牌，用于身份验证
            @RequestParam("dirName") String dirName, // 接收新目录名称
            @RequestParam(value = "parentDirId", required = false) Long parentDirId) throws Exception { // 父目录ID可选，不传则在根目录创建
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 从JWT令牌中提取用户ID，确定目录所属用户
        R_Directory directory = directoryService.createDirectory(userId, dirName, parentDirId); // 调用服务层创建目录实体
        return Result.success(directory); // 返回创建成功的目录信息
    }

    @PostMapping("/user/directory/list") // 列出指定目录下的子目录
    public Result<List<R_Directory>> listDirectories(
            @RequestHeader("Authorization") String authHeader, // JWT令牌用于识别用户身份
            @RequestParam(value = "parentDirId", required = false) Long parentDirId) throws Exception { // 父目录ID可选，不传则列出根目录内容
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 从令牌中解析用户ID，确保只查询当前用户的目录
        List<R_Directory> list = directoryService.listDirectories(userId, parentDirId); // 获取指定父目录下的所有子目录列表
        return Result.success(list); // 返回目录列表结果
    }

    @PostMapping("/user/directory/rename") // 重命名目录
    public Result<String> renameDirectory(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("dirId") Long dirId, // 要重命名的目标目录ID
            @RequestParam("newDirName") String newDirName) throws Exception { // 新的目录名称
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID，用于校验目录归属权限
        boolean ok = directoryService.renameDirectory(userId, dirId, newDirName); // 执行重命名操作，服务层会校验重名冲突
        if (ok) {
            return Result.success("目录重命名成功"); // 重命名成功，返回成功消息
        }
        return Result.error("目录重命名失败"); // 重命名失败（如名称冲突或目录不存在），返回错误提示
    }

    @PostMapping("/user/directory/delete") // 删除目录（移入回收站）
    public Result<String> deleteDirectory(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("dirId") Long dirId) throws Exception { // 要删除的目录ID
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID，校验目录归属
        boolean ok = directoryService.deleteDirectory(userId, dirId); // 执行软删除，将目录及其下文件移入回收站
        return ok ? Result.success("目录删除成功") : Result.error("目录删除失败"); // 根据操作结果返回相应提示
    }
}
