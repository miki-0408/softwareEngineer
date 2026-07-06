package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.Directory;
import org.example.netdisk.Entity.NetdiskFile;
import org.example.netdisk.Entity.StorageSpace;
import org.example.netdisk.Mapper.DirectoryMapper;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.Mapper.StorageSpaceMapper;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.Service.Inter.FileService;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Service.Support.TransformService;
import org.example.netdisk.Utils.CompressionUtil;
import org.example.netdisk.Utils.EncryptionUtil;
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

    @Override
    public R_File uploadFile(Long userId, Long dirId, MultipartFile file, boolean encrypt, String privatePassword) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        Directory directory = directoryMapper.selectDirectoryById(dirId, userId);
        if (directory == null) {
            throw new RuntimeException("目录不存在");
        }
        try {
            byte[] rawBytes = file.getBytes();
            int isEncryptedFlag = notEncrypted;
            if (encrypt) {
                privateSpaceService.validatePrivatePassword(userId, privatePassword);
                rawBytes = EncryptionUtil.encrypt(rawBytes, privatePassword);
                isEncryptedFlag = encrypted;
            }
            byte[] storedBytes = CompressionUtil.compress(rawBytes);
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
            netdiskFile.setDirId(dirId);
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

    @Override
    public byte[] downloadFile(Long userId, Long fileId, String privatePassword) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            throw new RuntimeException("文件不存在");
        }
        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        byte[] rawBytes = CompressionUtil.decompress(storedBytes);
        if (netdiskFile.getIsEncrypted() == encrypted) {
            privateSpaceService.validatePrivatePassword(userId, privatePassword);
            rawBytes = EncryptionUtil.decrypt(rawBytes, privatePassword);
        }
        return rawBytes;
    }

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
        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        byte[] rawBytes = CompressionUtil.decompress(storedBytes);
        byte[] encryptedBytes = EncryptionUtil.encrypt(rawBytes, privatePassword);
        byte[] newStoredBytes = CompressionUtil.compress(encryptedBytes);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());
        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);
        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);
        netdiskFile.setIsEncrypted(encrypted);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

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
        byte[] encryptedBytes = CompressionUtil.decompress(storedBytes);
        byte[] rawBytes = EncryptionUtil.decrypt(encryptedBytes, privatePassword);
        byte[] newStoredBytes = CompressionUtil.compress(rawBytes);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());
        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);
        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);
        netdiskFile.setIsEncrypted(notEncrypted);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    private void updateStorageUsed(StorageSpace storageSpace, long delta) {
        storageSpace.setUsedSpace(storageSpace.getUsedSpace() + delta);
        storageSpace.setRemainSpace(storageSpace.getTotalSpace() - storageSpace.getUsedSpace());
        storageSpaceMapper.updateStorageSpace(storageSpace);
    }

    private void adjustStorageOnSizeChange(Long userId, Long oldSize, long newSize) {
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
        if (storageSpace == null) {
            return;
        }
        long delta = newSize - oldSize;
        if (storageSpace.getRemainSpace() < delta) {
            throw new RuntimeException("存储空间不足");
        }
        updateStorageUsed(storageSpace, delta);
    }

    void releaseStorage(Long userId, Long fileSize) {
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
        if (storageSpace == null || fileSize == null) {
            return;
        }
        storageSpace.setUsedSpace(Math.max(0, storageSpace.getUsedSpace() - fileSize));
        storageSpace.setRemainSpace(storageSpace.getTotalSpace() - storageSpace.getUsedSpace());
        storageSpaceMapper.updateStorageSpace(storageSpace);
    }

    private String extractFileType(String fileName) {
        if (fileName == null) {
            return null;
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(idx + 1).toLowerCase();
    }
}
