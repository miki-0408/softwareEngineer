package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.R_Directory;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.ResponseDTO.R_PrivateSpace;
import org.example.netdisk.ResponseDTO.R_VerifyPrivateSpaceDTO;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Impl.FileServiceImpl;
import org.example.netdisk.Service.Inter.PrivateSpaceService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 私密空间控制器，管理用户的加密私人存储区域
public class PrivateSpaceController {

    @Autowired // 注入私密空间服务层
    private PrivateSpaceService privateSpaceService;
    @Autowired // 注入FileServiceImpl具体实现，用于浏览私密空间内的文件目录
    private FileServiceImpl fileService;

    @PostMapping("/user/privateSpace/enable") // 启用私密空间，设置访问密码
    public Result<String> enablePrivateSpace(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("password") String password) throws Exception { // 用户设置的私密空间密码
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 从令牌中提取用户ID
        boolean ok = privateSpaceService.enablePrivateSpace(userId, password); // 为该用户启用私密空间功能
        return ok ? Result.success("私密空间已启用") : Result.error("私密空间启用失败"); // 返回启用结果
    }

    @PostMapping("/user/privateSpace/disable") // 关闭私密空间，需验证密码
    public Result<String> disablePrivateSpace(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("password") String password) throws Exception { // 当前的私密空间密码，用于验证操作权限
        boolean ok = privateSpaceService.disablePrivateSpace(
                Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")), password); // 验证密码后关闭私密空间
        return ok ? Result.success("私密空间已关闭") : Result.error("私密空间关闭失败，密码错误或未启用"); // 密码错误或未启用时返回失败
    }

    @PostMapping("/user/privateSpace/verify") // 验证私密空间密码，用于前端解锁
    public Result<R_VerifyPrivateSpaceDTO> verifyPrivateSpace(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("password") String password) throws Exception { // 用户输入的私密空间密码
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 从令牌中提取用户ID
        R_VerifyPrivateSpaceDTO data = privateSpaceService.verifyPrivateSpace(userId, password); // 验证密码是否正确
        if (data == null) {
            return Result.error("私密空间未启用"); // 用户尚未启用私密空间功能
        }
        if (Boolean.TRUE.equals(data.getVerified())) {
            return Result.success(data); // 密码验证通过，返回令牌让前端记住验证状态
        }
        return Result.error("私密空间密码错误"); // 密码不正确
    }

    @PostMapping("/user/privateSpace/status") // 查询私密空间状态（是否启用、文件数量等）
    public Result<R_PrivateSpace> getPrivateSpaceStatus(
            @RequestHeader("Authorization") String authHeader) throws Exception { // JWT令牌验证用户身份
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID
        R_PrivateSpace status = privateSpaceService.getPrivateSpaceStatus(userId); // 获取私密空间的启用状态和统计信息
        return Result.success(status); // 返回状态信息
    }

    @PostMapping("/user/privateSpace/files") // 列出私密空间中指定目录下的所有文件
    public Result<List<R_File>> listPrivateFiles(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("dirId") Long dirId) throws Exception { // 私密空间内的目录ID
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID
        List<R_File> files = fileService.listPrivateFiles(userId, dirId); // 查询私密空间中该目录下的文件列表
        return Result.success(files); // 返回私密文件列表
    }

    @PostMapping("/user/privateSpace/directories") // 列出私密空间中指定父目录下的子目录
    public Result<List<R_Directory>> listPrivateDirectories(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam(value = "parentDirId", required = false) Long parentDirId) throws Exception { // 父目录ID，不传则列出根目录
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID
        List<R_Directory> dirs = fileService.listPrivateDirectories(userId, parentDirId); // 查询私密空间中的子目录列表
        return Result.success(dirs); // 返回私密目录列表
    }
}
