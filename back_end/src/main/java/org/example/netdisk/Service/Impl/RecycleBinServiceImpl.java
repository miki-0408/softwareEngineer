package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.Directory;
import org.example.netdisk.Entity.NetdiskFile;
import org.example.netdisk.Mapper.DirectoryMapper;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.Service.Inter.RecycleBinService;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Service.Support.TransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.example.netdisk.Service.Support.Enum.*;

@Slf4j
@Service
public class RecycleBinServiceImpl implements RecycleBinService {

    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private DirectoryMapper directoryMapper;
    @Autowired
    private PrivateSpaceServiceImpl privateSpaceService;
    @Autowired
    private FileServiceImpl fileService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private TransformService transformService;

    @Override
    public List<R_File> listRecycleFiles(Long userId) {
        List<NetdiskFile> files = fileMapper.selectFilesByStatus(userId, fileStatusRecycle);
        List<R_File> result = new ArrayList<>();
        for (NetdiskFile file : files) {
            result.add(transformService.transformFileToRFile(file));
        }
        return result;
    }

    @Override
    public boolean restoreFile(Long userId, Long fileId) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusRecycle) {
            return false;
        }
        // 如果原始目录已被删除或为空，回退到根目录
        boolean needRedirect = netdiskFile.getDirId() == null;
        if (!needRedirect && netdiskFile.getDirId() != null) {
            Directory dir = directoryMapper.selectDirectoryById(netdiskFile.getDirId(), userId);
            needRedirect = (dir == null);
        }
        if (needRedirect) {
            Long fallbackDirId = null;
            if (netdiskFile.getIsEncrypted() == encrypted) {
                Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
                fallbackDirId = privateRoot != null ? privateRoot.getDirId() : null;
            } else {
                Directory userRoot = directoryMapper.selectRootDirectory(userId);
                fallbackDirId = userRoot != null ? userRoot.getDirId() : null;
            }
            netdiskFile.setDirId(fallbackDirId);
            fileMapper.updateFile(netdiskFile);
        }
        return fileMapper.updateFileStatus(fileId, userId, fileStatusNormal) > 0;
    }

    @Override
    public boolean deletePermanently(Long userId, Long fileId) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusRecycle) {
            return false;
        }
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        fileService.releaseStorage(userId, netdiskFile.getFileSize());
        return fileMapper.deleteFile(fileId, userId) > 0;
    }
}
