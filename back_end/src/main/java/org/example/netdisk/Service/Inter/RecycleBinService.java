package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_File;

import java.util.List;

public interface RecycleBinService { // 回收站服务接口：管理被软删除的文件

    List<R_File> listRecycleFiles(Long userId); // 列出用户回收站中的文件

    boolean restoreFile(Long userId, Long fileId); // 从回收站恢复文件

    boolean deletePermanently(Long userId, Long fileId); // 从回收站永久删除文件（物理删除）
}
