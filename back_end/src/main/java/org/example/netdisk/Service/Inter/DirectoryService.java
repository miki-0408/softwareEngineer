package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_Directory;

import java.util.List;

public interface DirectoryService { // 目录服务接口：定义目录的增删改查操作

    R_Directory createDirectory(Long userId, String dirName, Long parentDirId); // 创建子目录，返回目录响应对象

    List<R_Directory> listDirectories(Long userId, Long parentDirId); // 列出指定父目录下的所有子目录

    boolean renameDirectory(Long userId, Long dirId, String newDirName); // 重命名目录

    boolean deleteDirectory(Long userId, Long dirId); // 删除目录（及其中所有文件和子目录）
}
