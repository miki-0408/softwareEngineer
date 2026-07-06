package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.Directory;
import org.example.netdisk.Entity.NetdiskFile;
import org.example.netdisk.Entity.StorageSpace;
import org.example.netdisk.Mapper.DirectoryMapper;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.Mapper.StorageSpaceMapper;
import org.example.netdisk.ResponseDTO.R_Directory;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.Service.Inter.FileService;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Service.Support.TransformService;
import org.example.netdisk.Utils.StandardCompression;
import org.example.netdisk.Utils.StandardEncryption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.example.netdisk.Service.Support.Enum.*;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private DirectoryMapper directoryMapper;
    @Autowired
    private StorageSpaceMapper storageSpaceMapper;
    @Autowired
    private PrivateSpaceServiceImpl privateSpaceService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private TransformService transformService;

    // ==================== 文件上传 ====================

    @Override
    public R_File uploadFile(Long userId, Long dirId, MultipartFile file, boolean encrypt, String privatePassword) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        Long targetDirId = dirId;
        int isEncryptedFlag = notEncrypted;
        Long originalDirId = null;

        if (encrypt) {
            privateSpaceService.validatePrivatePassword(userId, privatePassword);
            Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
            if (privateRoot == null) {
                throw new RuntimeException("私密空间根目录不存在，请先启用私密空间");
            }
            targetDirId = privateRoot.getDirId();
            isEncryptedFlag = encrypted;
            originalDirId = dirId;  // 记录原始目录
        } else {
            Directory directory = directoryMapper.selectDirectoryById(dirId, userId);
            if (directory == null) {
                throw new RuntimeException("目录不存在");
            }
        }

        try {
            byte[] rawBytes = file.getBytes();
            if (encrypt) {
                rawBytes = StandardEncryption.encrypt(rawBytes, privatePassword);
            }
            byte[] storedBytes = StandardCompression.compress(rawBytes);
            long storedSize = storedBytes.length;

            StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
            if (storageSpace == null || storageSpace.getRemainSpace() < storedSize) {
                throw new RuntimeException("存储空间不足");
            }

            NetdiskFile netdiskFile = new NetdiskFile();
            netdiskFile.setFileName(Objects.requireNonNullElse(file.getOriginalFilename(), "未命名文件"));
            netdiskFile.setFileType(extractFileType(netdiskFile.getFileName()));
            netdiskFile.setFileSize(storedSize);
            netdiskFile.setUserId(userId);
            netdiskFile.setDirId(targetDirId);
            netdiskFile.setOriginalDirId(originalDirId);
            netdiskFile.setStatus(fileStatusNormal);
            netdiskFile.setIsEncrypted(isEncryptedFlag);
            fileMapper.insertFile(netdiskFile);

            String path = fileStorageService.saveCompressedFile(storedBytes, userId, netdiskFile.getFileId());
            netdiskFile.setPath(path);
            fileMapper.updateFile(netdiskFile);

            updateStorageUsed(storageSpace, storedSize);
            return transformService.transformFileToRFile(netdiskFile);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    // ==================== 文件下载 ====================

    @Override
    public byte[] downloadFile(Long userId, Long fileId, String privatePassword) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            throw new RuntimeException("文件不存在");
        }
        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        byte[] rawBytes = StandardCompression.decompress(storedBytes);
        if (netdiskFile.getIsEncrypted() == encrypted) {
            privateSpaceService.validatePrivatePassword(userId, privatePassword);
            rawBytes = StandardEncryption.decrypt(rawBytes, privatePassword);
        }
        return rawBytes;
    }

    // ==================== 普通文件列表（自动排除已加密文件） ====================

    @Override
    public List<R_File> listFiles(Long userId, Long dirId) {
        Directory directory = directoryMapper.selectDirectoryById(dirId, userId);
        if (directory == null) {
            throw new RuntimeException("目录不存在");
        }
        List<NetdiskFile> files = fileMapper.selectFilesByDirAndStatus(userId, dirId, fileStatusNormal);
        List<R_File> result = new ArrayList<>();
        for (NetdiskFile file : files) {
            result.add(transformService.transformFileToRFile(file));
        }
        return result;
    }

    // ==================== 私密空间文件列表 ====================

    public List<R_File> listPrivateFiles(Long userId, Long dirId) {
        List<NetdiskFile> files = fileMapper.selectPrivateFilesByDirAndStatus(userId, dirId, fileStatusNormal);
        List<R_File> result = new ArrayList<>();
        for (NetdiskFile file : files) {
            result.add(transformService.transformFileToRFile(file));
        }
        return result;
    }

    // ==================== 私密空间目录列表 ====================

    public List<R_Directory> listPrivateDirectories(Long userId, Long parentDirId) {
        List<Directory> directories;
        if (parentDirId == null) {
            // 返回私密空间根目录
            Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
            if (privateRoot == null) return List.of();
            return List.of(transformService.transformDirectoryToRDirectory(privateRoot));
        }
        directories = directoryMapper.selectDirectoriesByParentId(userId, parentDirId);
        List<R_Directory> result = new ArrayList<>();
        for (Directory d : directories) {
            result.add(transformService.transformDirectoryToRDirectory(d));
        }
        return result;
    }

    // ==================== 重命名 / 移动 / 删除 ====================

    @Override
    public boolean renameFile(Long userId, Long fileId, String newFileName) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        netdiskFile.setFileName(newFileName);
        netdiskFile.setFileType(extractFileType(newFileName));
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    @Override
    public boolean moveFile(Long userId, Long fileId, Long targetDirId) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        Directory targetDir = directoryMapper.selectDirectoryById(targetDirId, userId);
        if (targetDir == null) {
            return false;
        }
        netdiskFile.setDirId(targetDirId);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    @Override
    public boolean moveToRecycleBin(Long userId, Long fileId) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        return fileMapper.updateFileStatus(fileId, userId, fileStatusRecycle) > 0;
    }

    // ==================== 移入私密空间（加密 + 移动到私密根目录） ====================

    @Override
    public boolean encryptFile(Long userId, Long fileId, String privatePassword) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        if (netdiskFile.getIsEncrypted() == encrypted) {
            return true;
        }
        privateSpaceService.validatePrivatePassword(userId, privatePassword);
        Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
        if (privateRoot == null) {
            throw new RuntimeException("私密空间根目录不存在");
        }

        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        byte[] rawBytes = StandardCompression.decompress(storedBytes);
        byte[] encryptedBytes = StandardEncryption.encrypt(rawBytes, privatePassword);
        byte[] newStoredBytes = StandardCompression.compress(encryptedBytes);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());

        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);

        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);
        netdiskFile.setOriginalDirId(netdiskFile.getDirId());  // 记住来源目录
        netdiskFile.setDirId(privateRoot.getDirId());          // 移到私密空间
        netdiskFile.setIsEncrypted(encrypted);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    // ==================== 移出私密空间（解密 + 恢复到原目录） ====================

    @Override
    public boolean decryptFile(Long userId, Long fileId, String privatePassword) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        if (netdiskFile.getIsEncrypted() != encrypted) {
            return true;
        }
        privateSpaceService.validatePrivatePassword(userId, privatePassword);

        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        byte[] encryptedBytes = StandardCompression.decompress(storedBytes);
        byte[] rawBytes = StandardEncryption.decrypt(encryptedBytes, privatePassword);
        byte[] newStoredBytes = StandardCompression.compress(rawBytes);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());

        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);

        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);

        // 恢复目标目录：优先用原始目录；如果是私密空间内部目录或为空，则恢复到"我的文件"
        Long originalDir = netdiskFile.getOriginalDirId();
        Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
        if (originalDir == null || (privateRoot != null && originalDir.equals(privateRoot.getDirId()))) {
            // 从私密空间直接上传的文件 → 恢复到用户根目录
            Directory userRoot = directoryMapper.selectRootDirectory(userId);
            originalDir = userRoot != null ? userRoot.getDirId() : null;
        }
        netdiskFile.setDirId(originalDir);
        netdiskFile.setOriginalDirId(null);
        netdiskFile.setIsEncrypted(notEncrypted);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    // ==================== 工具方法 ====================

    private void updateStorageUsed(StorageSpace storageSpace, long delta) {
        storageSpace.setUsedSpace(storageSpace.getUsedSpace() + delta);
        storageSpace.setRemainSpace(storageSpace.getTotalSpace() - storageSpace.getUsedSpace());
        storageSpaceMapper.updateStorageSpace(storageSpace);
    }

    private void adjustStorageOnSizeChange(Long userId, Long oldSize, long newSize) {
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
        if (storageSpace == null) return;
        long delta = newSize - oldSize;
        if (storageSpace.getRemainSpace() < delta) {
            throw new RuntimeException("存储空间不足");
        }
        updateStorageUsed(storageSpace, delta);
    }

    void releaseStorage(Long userId, Long fileSize) {
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
        if (storageSpace == null || fileSize == null) return;
        storageSpace.setUsedSpace(Math.max(0, storageSpace.getUsedSpace() - fileSize));
        storageSpace.setRemainSpace(storageSpace.getTotalSpace() - storageSpace.getUsedSpace());
        storageSpaceMapper.updateStorageSpace(storageSpace);
    }

    private String extractFileType(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1).toLowerCase();
    }
}
