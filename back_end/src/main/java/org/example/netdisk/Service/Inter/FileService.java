package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_File;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    R_File uploadFiles(Long userId, Long dirId, List<MultipartFile> files, List<String> relativePaths,
                       boolean encrypt, String privatePassword, String packMethod, String compressMethod,
                       String displayName);

    byte[] downloadFile(Long userId, Long fileId, String privatePassword);

    List<R_File> listFiles(Long userId, Long dirId);

    boolean renameFile(Long userId, Long fileId, String newFileName, boolean force);

    boolean moveFile(Long userId, Long fileId, Long targetDirId, boolean force);

    boolean moveToRecycleBin(Long userId, Long fileId);

    boolean encryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId, boolean force);

    boolean decryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId, boolean force);
}
