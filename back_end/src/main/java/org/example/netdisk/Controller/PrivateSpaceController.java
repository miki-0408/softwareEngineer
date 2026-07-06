package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.R_PrivateSpace;
import org.example.netdisk.ResponseDTO.R_VerifyPrivateSpaceDTO;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.PrivateSpaceService;
import org.example.netdisk.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PrivateSpaceController {

    @Autowired
    private PrivateSpaceService privateSpaceService;

    @PostMapping("/user/privateSpace/enable")
    public Result<String> enablePrivateSpace(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("password") String password) throws Exception {
        try {
            Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
            boolean ok = privateSpaceService.enablePrivateSpace(userId, password);
            if (ok) {
                return Result.success("私密空间已启用");
            }
            return Result.error("私密空间启用失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/user/privateSpace/disable")
    public Result<String> disablePrivateSpace(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("password") String password) throws Exception {
        boolean ok = privateSpaceService.disablePrivateSpace(
                Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId")), password);
        if (ok) {
            return Result.success("私密空间已关闭");
        }
        return Result.error("私密空间关闭失败，密码错误或未启用");
    }

    @PostMapping("/user/privateSpace/verify")
    public Result<R_VerifyPrivateSpaceDTO> verifyPrivateSpace(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("password") String password) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        R_VerifyPrivateSpaceDTO data = privateSpaceService.verifyPrivateSpace(userId, password);
        if (data == null) {
            return Result.error("私密空间未启用");
        }
        if (Boolean.TRUE.equals(data.getVerified())) {
            return Result.success(data);
        }
        return Result.error("私密空间密码错误");
    }

    @PostMapping("/user/privateSpace/status")
    public Result<R_PrivateSpace> getPrivateSpaceStatus(
            @RequestHeader("Authorization") String authHeader) throws Exception {
        Long userId = Long.parseLong((String) TokenProcess.getAttributeFromToken(authHeader, "userId"));
        R_PrivateSpace status = privateSpaceService.getPrivateSpaceStatus(userId);
        return Result.success(status);
    }
}
