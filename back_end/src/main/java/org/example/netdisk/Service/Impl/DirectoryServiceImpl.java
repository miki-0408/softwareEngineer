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
    private DirectoryMapper directoryMapper; // 目录表数据库操作接口
    @Autowired
    private FileMapper fileMapper; // 文件表数据库操作接口，用于检查目录下是否有文件
    @Autowired
    private TransformService transformService; // 实体与响应DTO之间的转换服务

    private String uniqueDirName(Long userId, Long parentDirId, String dirName) { // 生成唯一目录名，防止同级目录重名
        String candidate = dirName; // 初始候选名即为原始目录名
        int n = 1; // 重名计数器，从1开始递增
        while (directoryMapper.countDirsByName(userId, parentDirId, candidate) > 0) { // 查询同名目录是否存在
            candidate = dirName + " (" + n + ")"; // 存在同名则在名称后追加序号
            n++; // 递增计数器，准备下一轮尝试
        }
        return candidate; // 返回唯一的目录名称
    }

    @Override
    public R_Directory createDirectory(Long userId, String dirName, Long parentDirId) { // 在指定父目录下创建新目录
        if (parentDirId != null) { // 如果指定了父目录ID，需要验证父目录是否存在
            Directory parent = directoryMapper.selectDirectoryById(parentDirId, userId); // 查询父目录，同时校验归属用户
            if (parent == null) { // 父目录不存在或不属于当前用户
                throw new RuntimeException("父目录不存在"); // 抛出业务异常，中断创建流程
            }
        }
        dirName = uniqueDirName(userId, parentDirId, dirName); // 确保目录名在同级目录中不重复
        Directory directory = new Directory(); // 构造新目录实体对象
        directory.setDirName(dirName); // 设置目录名称（已去重处理）
        directory.setParentDirId(parentDirId); // 设置父目录ID，null表示根目录
        directory.setUserId(userId); // 绑定目录归属用户
        directoryMapper.insertDirectory(directory); // 将新目录记录插入数据库
        return transformService.transformDirectoryToRDirectory(directory); // 将实体转换为响应DTO返回给前端
    }

    @Override
    public List<R_Directory> listDirectories(Long userId, Long parentDirId) { // 列出指定父目录下的所有子目录
        List<Directory> directories; // 存储查询结果
        if (parentDirId == null) { // parentDirId为空表示查询根目录列表
            directories = directoryMapper.selectRootDirectories(userId); // 查询该用户的所有根级目录
        } else { // parentDirId不为空，查询该目录的子目录
            directories = directoryMapper.selectDirectoriesByParentId(userId, parentDirId); // 按父目录ID查询子目录列表
        }
        List<R_Directory> result = new ArrayList<>(); // 创建响应DTO列表
        for (Directory directory : directories) { // 遍历所有查询到的目录实体
            result.add(transformService.transformDirectoryToRDirectory(directory)); // 逐个转换为响应DTO并加入结果集
        }
        return result; // 返回子目录列表
    }

    @Override
    public boolean renameDirectory(Long userId, Long dirId, String newDirName) { // 重命名指定目录
        Directory directory = directoryMapper.selectDirectoryById(dirId, userId); // 查询目录并校验归属用户
        if (directory == null) { // 目录不存在或不属于当前用户
            return false; // 重命名失败
        }
        newDirName = uniqueDirName(userId, directory.getParentDirId(), newDirName); // 生成不重复的新名称
        directory.setDirName(newDirName); // 更新目录名为去重后的新名称
        return directoryMapper.updateDirectoryName(directory) > 0; // 执行更新并返回是否成功（影响行数>0）
    }

    @Override
    public boolean deleteDirectory(Long userId, Long dirId) { // 删除指定目录（仅允许删除空目录）
        Directory directory = directoryMapper.selectDirectoryById(dirId, userId); // 查询目录并校验归属用户
        if (directory == null) { // 目录不存在或不属于当前用户
            return false; // 删除失败
        }
        if (directory.getParentDirId() == null) { // 父目录ID为空说明是根目录
            throw new RuntimeException("根目录不可删除"); // 根目录受保护，不允许删除
        }
        List<Directory> children = directoryMapper.selectDirectoriesByParentId(userId, dirId); // 查询该目录下是否有子目录
        if (!children.isEmpty()) { // 子目录列表非空，说明存在子目录
            throw new RuntimeException("目录下存在子目录，无法删除"); // 有子目录时拒绝删除，保证数据完整性
        }
        if (fileMapper.countFilesInDirectory(userId, dirId) > 0) { // 统计该目录下的文件数量
            throw new RuntimeException("目录下存在文件，无法删除"); // 目录非空，拒绝删除以避免孤立文件
        }
        return directoryMapper.deleteDirectory(dirId, userId) > 0; // 执行物理删除并返回是否成功
    }
}
