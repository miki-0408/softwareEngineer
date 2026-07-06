package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.R_Directory;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.DirectoryService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DirectoryController {

    @Autowired
    private DirectoryService directoryService;

    @PostMapping("/user/directory/create")
    public Result<R_Directory> createDirectory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("dirName") String dirName,
            @RequestParam(value = "parentDirId", required = false) Long parentDirId) throws Exception {
        try {
            Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
            R_Directory directory = directoryService.createDirectory(userId, dirName, parentDirId);
            return Result.success(directory);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/user/directory/list")
    public Result<List<R_Directory>> listDirectories(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "parentDirId", required = false) Long parentDirId) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        List<R_Directory> list = directoryService.listDirectories(userId, parentDirId);
        return Result.success(list);
    }

    @PostMapping("/user/directory/rename")
    public Result<String> renameDirectory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("dirId") Long dirId,
            @RequestParam("newDirName") String newDirName) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        boolean ok = directoryService.renameDirectory(userId, dirId, newDirName);
        if (ok) {
            return Result.success("目录重命名成功");
        }
        return Result.error("目录重命名失败");
    }

    @PostMapping("/user/directory/delete")
    public Result<String> deleteDirectory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("dirId") Long dirId) throws Exception {
        try {
            Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
            boolean ok = directoryService.deleteDirectory(userId, dirId);
            if (ok) {
                return Result.success("目录删除成功");
            }
            return Result.error("目录删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
