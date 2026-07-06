package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.Directory;
import org.example.netdisk.Mapper.DirectoryMapper;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.ResponseDTO.R_Directory;
import org.example.netdisk.Service.Inter.DirectoryService;
import org.example.netdisk.Service.Support.TransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DirectoryServiceImpl implements DirectoryService {

    @Autowired
    private DirectoryMapper directoryMapper;
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private TransformService transformService;

    @Override
    public R_Directory createDirectory(Long userId, String dirName, Long parentDirId) {
        if (parentDirId != null) {
            Directory parent = directoryMapper.selectDirectoryById(parentDirId, userId);
            if (parent == null) {
                throw new RuntimeException("父目录不存在");
            }
        }
        Directory directory = new Directory();
        directory.setDirName(dirName);
        directory.setParentDirId(parentDirId);
        directory.setUserId(userId);
        directoryMapper.insertDirectory(directory);
        return transformService.transformDirectoryToRDirectory(directory);
    }

    @Override
    public List<R_Directory> listDirectories(Long userId, Long parentDirId) {
        List<Directory> directories;
        if (parentDirId == null) {
            directories = directoryMapper.selectRootDirectories(userId);
        } else {
            directories = directoryMapper.selectDirectoriesByParentId(userId, parentDirId);
        }
        List<R_Directory> result = new ArrayList<>();
        for (Directory directory : directories) {
            result.add(transformService.transformDirectoryToRDirectory(directory));
        }
        return result;
    }

    @Override
    public boolean renameDirectory(Long userId, Long dirId, String newDirName) {
        Directory directory = directoryMapper.selectDirectoryById(dirId, userId);
        if (directory == null) {
            return false;
        }
        directory.setDirName(newDirName);
        return directoryMapper.updateDirectoryName(directory) > 0;
    }

    @Override
    public boolean deleteDirectory(Long userId, Long dirId) {
        Directory directory = directoryMapper.selectDirectoryById(dirId, userId);
        if (directory == null) {
            return false;
        }
        if (directory.getParentDirId() == null) {
            throw new RuntimeException("根目录不可删除");
        }
        List<Directory> children = directoryMapper.selectDirectoriesByParentId(userId, dirId);
        if (!children.isEmpty()) {
            throw new RuntimeException("目录下存在子目录，无法删除");
        }
        if (fileMapper.countFilesInDirectory(userId, dirId) > 0) {
            throw new RuntimeException("目录下存在文件，无法删除");
        }
        return directoryMapper.deleteDirectory(dirId, userId) > 0;
    }
}
