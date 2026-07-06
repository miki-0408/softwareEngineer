package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.RecycleBinService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RecycleBinController {

    @Autowired
    private RecycleBinService recycleBinService;

    @PostMapping("/user/recycle/list")
    public Result<List<R_File>> listRecycleFiles(
            @RequestHeader("Authorization") String authHeader) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        List<R_File> list = recycleBinService.listRecycleFiles(userId);
        return Result.success(list);
    }

    @PostMapping("/user/recycle/restore")
    public Result<String> restoreFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fileId") Long fileId) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        boolean ok = recycleBinService.restoreFile(userId, fileId);
        if (ok) {
            return Result.success("文件恢复成功");
        }
        return Result.error("文件恢复失败");
    }

    @PostMapping("/user/recycle/deletePermanent")
    public Result<String> deletePermanently(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("fileId") Long fileId) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        boolean ok = recycleBinService.deletePermanently(userId, fileId);
        if (ok) {
            return Result.success("文件已彻底删除");
        }
        return Result.error("彻底删除失败");
    }
}
