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
    private FileMapper fileMapper; // 文件表数据库操作接口
    @Autowired
    private DirectoryMapper directoryMapper; // 目录表数据库操作接口，用于还原时查找目录
    @Autowired
    private PrivateSpaceServiceImpl privateSpaceService; // 私密空间服务，用于还原加密文件时找私密空间根目录
    @Autowired
    private FileServiceImpl fileService; // 文件服务，用于调用uniqueFileName和releaseStorage
    @Autowired
    private FileStorageService fileStorageService; // 物理文件存储服务，用于永久删除时清理磁盘文件
    @Autowired
    private TransformService transformService; // 实体与响应DTO之间的转换服务

    @Override
    public List<R_File> listRecycleFiles(Long userId) { // 列出用户回收站中的所有文件
        List<NetdiskFile> files = fileMapper.selectFilesByStatus(userId, fileStatusRecycle); // 查询状态为回收站的所有文件
        List<R_File> result = new ArrayList<>(); // 创建响应DTO列表
        for (NetdiskFile file : files) { // 遍历所有回收站文件
            result.add(transformService.transformFileToRFile(file)); // 逐个转换为响应DTO并加入结果集
        }
        return result; // 返回回收站文件列表
    }

    @Override
    public boolean restoreFile(Long userId, Long fileId) { // 从回收站还原文件：查记录→修正目录→处理重名→改状态
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId); // 查询文件记录并校验归属用户
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusRecycle) { // 文件不存在或不在回收站
            return false; // 还原失败
        }
        boolean needRedirect = netdiskFile.getDirId() == null; // 原始目录ID为空，需要回退
        if (!needRedirect && netdiskFile.getDirId() != null) { // 原始目录ID不为空，检查目录是否还存在
            Directory dir = directoryMapper.selectDirectoryById(netdiskFile.getDirId(), userId); // 查询原始目录是否存在
            needRedirect = (dir == null); // 目录已被删除，需要重新指定目标目录
        }
        if (needRedirect) { // 原始目录不存在或为空，需要回退到合适的根目录
            Long fallbackDirId = null; // 回退目录ID
            if (netdiskFile.getIsEncrypted() == encrypted) { // 加密文件回退到私密空间根目录
                Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId); // 查找私密空间根目录
                fallbackDirId = privateRoot != null ? privateRoot.getDirId() : null; // 获取根目录ID，不存在则保持null
            } else { // 普通文件回退到用户根目录
                Directory userRoot = directoryMapper.selectRootDirectory(userId); // 查询用户根目录
                fallbackDirId = userRoot != null ? userRoot.getDirId() : null; // 获取根目录ID，不存在则保持null
            }
            netdiskFile.setDirId(fallbackDirId); // 设置回退后的目标目录ID
        }
        Long targetDirId = netdiskFile.getDirId(); // 获取最终的目标目录ID
        if (targetDirId != null) { // 目标目录ID有效，检查是否需要处理同名冲突
            String uniqueName = fileService.uniqueFileName(userId, targetDirId, netdiskFile.getFileName()); // 为目标目录生成唯一文件名
            if (!uniqueName.equals(netdiskFile.getFileName())) { // 同名文件已存在，需要重命名
                netdiskFile.setFileName(uniqueName); // 使用带序号的唯一文件名
            }
        }
        fileMapper.updateFile(netdiskFile); // 更新文件记录（可能修改了dirId和fileName）
        return fileMapper.updateFileStatus(fileId, userId, fileStatusNormal) > 0; // 将文件状态改回正常并返回结果
    }

    @Override
    public boolean deletePermanently(Long userId, Long fileId) { // 永久删除文件：查记录→删物理文件→释放空间→删数据库记录
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId); // 查询文件记录并校验归属用户
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusRecycle) { // 文件不存在或不在回收站
            return false; // 永久删除失败
        }
        fileStorageService.deleteStoredFile(netdiskFile.getPath()); // 从磁盘上删除物理文件，释放磁盘空间
        fileService.releaseStorage(userId, netdiskFile.getFileSize()); // 从用户的存储记录中减去该文件占用的空间
        return fileMapper.deleteFile(fileId, userId) > 0; // 从数据库删除文件记录并返回是否成功
    }
}
