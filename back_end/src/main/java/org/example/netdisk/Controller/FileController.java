package org.example.netdisk.Controller;

import org.example.netdisk.Entity.NetdiskFile;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.FileService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController // REST控制器，所有响应自动JSON序列化
public class FileController {

    @Autowired // 注入文件服务层
    private FileService fileService;
    @Autowired // 注入文件数据访问层，用于直接查询文件元数据
    private FileMapper fileMapper;

    @PostMapping("/user/file/upload") // 文件上传API，支持多文件、加密、压缩、打包等参数
    public Result<R_File> uploadFile(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("dirId") Long dirId, // 上传到的目标目录ID
            @RequestParam("files") List<MultipartFile> files, // 接收上传的文件列表
            @RequestParam(value = "relativePaths", required = false) List<String> relativePaths, // 可选：文件相对路径列表，用于保留目录结构
            @RequestParam(value = "encrypt", defaultValue = "false") boolean encrypt, // 是否对文件进行加密，默认不加密
            @RequestParam(value = "privatePassword", required = false) String privatePassword, // 加密密码，仅在加密时使用
            @RequestParam(value = "packMethod", defaultValue = "none") String packMethod, // 打包方式：none不打包、zip打包等
            @RequestParam(value = "compressMethod", defaultValue = "lz77") String compressMethod, // 压缩算法：lz77默认压缩
            @RequestParam(value = "displayName", required = false) String displayName) throws Exception { // 可选：文件显示名称
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 从令牌中提取用户ID
        R_File rFile = fileService.uploadFiles(userId, dirId, files, relativePaths, encrypt, privatePassword, packMethod, compressMethod, displayName); // 委托服务层处理文件存储、加密、压缩全流程
        return Result.success(rFile); // 返回上传结果，包含文件ID、大小等元数据
    }

    @PostMapping("/user/file/download") // 文件下载API，返回字节流而非JSON，支持私密文件密码验证
    public ResponseEntity<byte[]> downloadFile(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("fileId") Long fileId, // 要下载的文件ID
            @RequestParam(value = "privatePassword", required = false) String privatePassword) throws Exception { // 私密文件解密密码
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID，校验文件归属
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId); // 查询文件元数据，获取文件名用于下载响应头
        if (netdiskFile == null) {
            return ResponseEntity.notFound().build(); // 文件不存在时返回404状态码
        }
        byte[] data = fileService.downloadFile(userId, fileId, privatePassword); // 获取文件字节数据，服务层会处理解密和解压缩
        String encodedName = URLEncoder.encode(netdiskFile.getFileName(), StandardCharsets.UTF_8).replace("+", "%20"); // 对中文文件名进行URL编码，确保浏览器正确识别文件名
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName) // 设置Content-Disposition头，触发浏览器下载并保留中文文件名
                .contentType(MediaType.APPLICATION_OCTET_STREAM) // 设置为通用二进制流类型，让浏览器按下载方式处理
                .body(data); // 返回文件字节内容
    }

    @PostMapping("/user/file/list") // 列出指定目录下的文件列表
    public Result<List<R_File>> listFiles(
            @RequestHeader("Authorization") String authHeader, // JWT令牌识别用户身份
            @RequestParam("dirId") Long dirId) throws Exception { // 目标目录ID
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 从令牌中提取用户ID
        List<R_File> list = fileService.listFiles(userId, dirId); // 获取该目录下所有文件的响应DTO列表
        return Result.success(list); // 返回文件列表
    }

    @PostMapping("/user/file/rename") // 文件重命名API
    public Result<String> renameFile(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("fileId") Long fileId, // 要重命名的文件ID
            @RequestParam("newFileName") String newFileName, // 新文件名
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception { // 强制重命名，为true时覆盖同名文件
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID校验归属
        boolean ok = fileService.renameFile(userId, fileId, newFileName, force); // 执行重命名，force=true时自动覆盖重名冲突
        if (ok) {
            return Result.success("文件重命名成功"); // 重命名成功
        }
        return Result.error("文件重命名失败"); // 重命名失败（如名称冲突且未强制覆盖）
    }

    @PostMapping("/user/file/move") // 文件移动/迁移API，将文件从当前目录移到目标目录
    public Result<String> moveFile(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("fileId") Long fileId, // 要移动的文件ID
            @RequestParam("targetDirId") Long targetDirId, // 目标目录ID
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception { // 强制移动，遇上重名自动覆盖
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID
        boolean ok = fileService.moveFile(userId, fileId, targetDirId, force); // 执行文件迁移操作
        if (ok) {
            return Result.success("文件移动成功"); // 移动成功
        }
        return Result.error("文件移动失败"); // 移动失败（如目标目录不存在或重名冲突）
    }

    @PostMapping("/user/file/delete") // 文件删除API（软删除，移入回收站）
    public Result<String> deleteFile(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("fileId") Long fileId) throws Exception { // 要删除的文件ID
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID
        boolean ok = fileService.moveToRecycleBin(userId, fileId); // 将文件移入回收站而非直接物理删除，支持后续恢复
        if (ok) {
            return Result.success("文件已移入回收站"); // 删除成功，文件进入回收站
        }
        return Result.error("删除失败"); // 删除失败
    }

    @PostMapping("/user/file/encrypt") // 加密文件API，对已有文件进行加密处理
    public Result<String> encryptFile(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("fileId") Long fileId, // 要加密的文件ID
            @RequestParam("privatePassword") String privatePassword, // 加密密码
            @RequestParam("targetDirId") Long targetDirId, // 加密后文件存放的目标目录
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception { // 强制加密，覆盖同名冲突
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID
        boolean ok = fileService.encryptFile(userId, fileId, privatePassword, targetDirId, force); // 执行文件加密，生成加密副本到目标目录
        return ok ? Result.success("文件加密成功") : Result.error("文件加密失败"); // 返回加密结果
    }

    @PostMapping("/user/file/decrypt") // 解密文件API，对加密文件进行解密恢复
    public Result<String> decryptFile(
            @RequestHeader("Authorization") String authHeader, // JWT令牌验证用户身份
            @RequestParam("fileId") Long fileId, // 要解密的文件ID
            @RequestParam("privatePassword") String privatePassword, // 解密密码，须与加密时的密码一致
            @RequestParam("targetDirId") Long targetDirId, // 解密后文件存放的目标目录
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception { // 强制解密，覆盖同名冲突
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")); // 提取用户ID
        boolean ok = fileService.decryptFile(userId, fileId, privatePassword, targetDirId, force); // 执行解密操作，生成明文副本到目标目录
        return ok ? Result.success("文件已解除加密") : Result.error("解除加密失败"); // 返回解密结果
    }
}
