package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_File;

import java.util.List;

public interface RecycleBinService {

    List<R_File> listRecycleFiles(Long userId);

    boolean restoreFile(Long userId, Long fileId);

    boolean deletePermanently(Long userId, Long fileId);
}
