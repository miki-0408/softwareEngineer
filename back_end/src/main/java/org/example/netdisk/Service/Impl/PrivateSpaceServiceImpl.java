package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.*;
import org.example.netdisk.Mapper.*;
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
    private DirectoryMapper directoryMapper;
    @Autowired
    private FileMapper fileMapper;
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
        // 创建私密空间根目录（如果不存在）
        Directory existingRoot = findPrivateSpaceRoot(userId);
        if (existingRoot == null) {
            Directory privateRoot = new Directory();
            privateRoot.setDirName(privateSpaceRootDirName);
            privateRoot.setParentDirId(null);
            privateRoot.setUserId(userId);
            directoryMapper.insertDirectory(privateRoot);
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
        // 检查并删除私密空间根目录
        Directory privateRoot = findPrivateSpaceRoot(userId);
        if (privateRoot != null) {
            int dirCount = directoryMapper.selectDirectoriesByParentId(userId, privateRoot.getDirId()).size();
            int fileCount = fileMapper.countPrivateFilesInDirectory(userId, privateRoot.getDirId());
            if (dirCount > 0 || fileCount > 0) {
                throw new RuntimeException("私密空间中仍有文件，请先将所有文件移出后再关闭");
            }
            directoryMapper.deleteDirectory(privateRoot.getDirId(), userId);
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
        String token = JwtUtil.genToken(
            new Claims(String.valueOf(userId), user.getRole()).toMap()
        );
        R_VerifyPrivateSpaceDTO dto = new R_VerifyPrivateSpaceDTO();
        dto.setVerified(true);
        dto.setTempToken(token);
        return dto;
    }

    @Override
    public R_PrivateSpace getPrivateSpaceStatus(Long userId) {
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId);
        R_PrivateSpace result = transformService.transformPrivateSpaceToRPrivateSpace(privateSpace);
        // 自动创建私密空间根目录（兼容历史数据）
        Directory privateRoot = findPrivateSpaceRoot(userId);
        if (privateRoot == null && privateSpace != null && privateSpace.getIsEncrypted() == privateSpaceEnabled) {
            privateRoot = new Directory();
            privateRoot.setDirName(privateSpaceRootDirName);
            privateRoot.setParentDirId(null);
            privateRoot.setUserId(userId);
            directoryMapper.insertDirectory(privateRoot);
        }
        if (privateRoot != null) {
            result.setRootDirId(String.valueOf(privateRoot.getDirId()));
        }
        return result;
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

    public String getPrivateSpacePassword(Long userId) {
        PrivateSpace ps = privateSpaceMapper.selectByUserId(userId);
        if (ps == null) return null;
        return ps.getPassword();
    }

    /** 查找用户的私密空间根目录 */
    public Directory findPrivateSpaceRoot(Long userId) {
        java.util.List<Directory> roots = directoryMapper.selectRootDirectories(userId);
        return roots.stream()
            .filter(d -> privateSpaceRootDirName.equals(d.getDirName()))
            .findFirst().orElse(null);
    }
}
