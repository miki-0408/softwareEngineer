package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.*;
import org.example.netdisk.Mapper.*;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.ResponseDTO.R_PrivateSpace;
import org.example.netdisk.ResponseDTO.R_VerifyPrivateSpaceDTO;
import org.example.netdisk.Service.Inter.PrivateSpaceService;
import org.example.netdisk.Service.Support.TransformService;
import org.example.netdisk.Utils.BcryptUtil;
import org.example.netdisk.Utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.example.netdisk.Service.Support.Enum.*;

@Slf4j
@Service
public class PrivateSpaceServiceImpl implements PrivateSpaceService {

    @Autowired
    private PrivateSpaceMapper privateSpaceMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TransformService transformService;

    @Override
    public boolean enablePrivateSpace(Long userId, String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("私密空间密码不能为空");
        }
        PrivateSpace existing = privateSpaceMapper.selectByUserId(userId);
        if (existing != null && existing.getIsEncrypted() == privateSpaceEnabled) {
            throw new RuntimeException("私密空间已启用");
        }
        PrivateSpace privateSpace = new PrivateSpace();
        privateSpace.setUserId(userId);
        privateSpace.setPassword(BcryptUtil.hash(password));
        privateSpace.setIsEncrypted(privateSpaceEnabled);
        if (existing == null) {
            privateSpaceMapper.insertPrivateSpace(privateSpace);
        } else {
            privateSpaceMapper.updatePrivateSpace(privateSpace);
        }
        return true;
    }

    @Override
    public boolean disablePrivateSpace(Long userId, String password) {
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId);
        if (privateSpace == null || privateSpace.getIsEncrypted() != privateSpaceEnabled) {
            return false;
        }
        if (!BcryptUtil.matches(password, privateSpace.getPassword())) {
            return false;
        }
        privateSpace.setIsEncrypted(privateSpaceDisabled);
        privateSpaceMapper.updatePrivateSpace(privateSpace);
        return true;
    }

    @Override
    public R_VerifyPrivateSpaceDTO verifyPrivateSpace(Long userId, String password) {
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId);
        if (privateSpace == null || privateSpace.getIsEncrypted() != privateSpaceEnabled) {
            return null;
        }
        if (!BcryptUtil.matches(password, privateSpace.getPassword())) {
            R_VerifyPrivateSpaceDTO dto = new R_VerifyPrivateSpaceDTO();
            dto.setVerified(false);
            return dto;
        }
        User user = userMapper.selectUserById(userId);
        String token = JwtUtil.genToken(new Claims(user.getName(), String.valueOf(userId), user.getRole(), privateSpaceVerify).toMap());
        R_VerifyPrivateSpaceDTO dto = new R_VerifyPrivateSpaceDTO();
        dto.setVerified(true);
        dto.setTempToken(token);
        return dto;
    }

    @Override
    public R_PrivateSpace getPrivateSpaceStatus(Long userId) {
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId);
        return transformService.transformPrivateSpaceToRPrivateSpace(privateSpace);
    }

    public void validatePrivatePassword(Long userId, String privatePassword) {
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId);
        if (privateSpace == null || privateSpace.getIsEncrypted() != privateSpaceEnabled) {
            throw new RuntimeException("请先启用私密空间");
        }
        if (privatePassword == null || !BcryptUtil.matches(privatePassword, privateSpace.getPassword())) {
            throw new RuntimeException("私密空间密码错误");
        }
    }
}
