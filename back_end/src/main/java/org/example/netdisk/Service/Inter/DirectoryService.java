package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_Directory;

import java.util.List;

public interface DirectoryService {

    R_Directory createDirectory(Long userId, String dirName, Long parentDirId);

    List<R_Directory> listDirectories(Long userId, Long parentDirId);

    boolean renameDirectory(Long userId, Long dirId, String newDirName);

    boolean deleteDirectory(Long userId, Long dirId);
}
