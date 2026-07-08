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
import java.util.ArrayList;
import java.util.List;

@RestController
public class FileController {

    @Autowired
    private FileService fileService;
    @Autowired
    private FileMapper fileMapper;

    @PostMapping("/user/file/upload")
    public Result<R_File> uploadFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("dirId") Long dirId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "relativePaths", required = false) List<String> relativePaths,
            @RequestParam(value = "encrypt", defaultValue = "false") boolean encrypt,
            @RequestParam(value = "privatePassword", required = false) String privatePassword,
            @RequestParam(value = "packMethod", defaultValue = "none") String packMethod,
            @RequestParam(value = "compressMethod", defaultValue = "lz77") String compressMethod,
            @RequestParam(value = "displayName", required = false) String displayName) throws Exception {
        try {
            Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
            R_File rFile = fileService.uploadFiles(userId, dirId, files, relativePaths, encrypt, privatePassword, packMethod, compressMethod, displayName);
            return Result.success(rFile);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/user/file/download")
    public ResponseEntity<byte[]> downloadFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fileId") Long fileId,
            @RequestParam(value = "privatePassword", required = false) String privatePassword) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = fileService.downloadFile(userId, fileId, privatePassword);
        String encodedName = URLEncoder.encode(netdiskFile.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/user/file/list")
    public Result<List<R_File>> listFiles(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("dirId") Long dirId) throws Exception {
        try {
            Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
            List<R_File> list = fileService.listFiles(userId, dirId);
            return Result.success(list);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/user/file/rename")
    public Result<String> renameFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fileId") Long fileId,
            @RequestParam("newFileName") String newFileName) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        boolean ok = fileService.renameFile(userId, fileId, newFileName);
        if (ok) {
            return Result.success("文件重命名成功");
        }
        return Result.error("文件重命名失败");
    }

    @PostMapping("/user/file/move")
    public Result<String> moveFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fileId") Long fileId,
            @RequestParam("targetDirId") Long targetDirId) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        boolean ok = fileService.moveFile(userId, fileId, targetDirId);
        if (ok) {
            return Result.success("文件移动成功");
        }
        return Result.error("文件移动失败");
    }

    @PostMapping("/user/file/delete")
    public Result<String> deleteFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fileId") Long fileId) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        boolean ok = fileService.moveToRecycleBin(userId, fileId);
        if (ok) {
            return Result.success("文件已移入回收站");
        }
        return Result.error("删除失败");
    }

    @PostMapping("/user/file/encrypt")
    public Result<String> encryptFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fileId") Long fileId,
            @RequestParam("privatePassword") String privatePassword,
            @RequestParam("targetDirId") Long targetDirId) throws Exception {
        try {
            Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
            boolean ok = fileService.encryptFile(userId, fileId, privatePassword, targetDirId);
            if (ok) {
                return Result.success("文件加密成功");
            }
            return Result.error("文件加密失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/user/file/decrypt")
    public Result<String> decryptFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fileId") Long fileId,
            @RequestParam("privatePassword") String privatePassword,
            @RequestParam("targetDirId") Long targetDirId) throws Exception {
        try {
            Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
            boolean ok = fileService.decryptFile(userId, fileId, privatePassword, targetDirId);
            if (ok) {
                return Result.success("文件已解除加密");
            }
            return Result.error("解除加密失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
