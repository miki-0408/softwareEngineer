package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_File;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    R_File uploadFile(Long userId, Long dirId, MultipartFile file, boolean encrypt, String privatePassword);

    byte[] downloadFile(Long userId, Long fileId, String privatePassword);

    List<R_File> listFiles(Long userId, Long dirId);

    boolean renameFile(Long userId, Long fileId, String newFileName);

    boolean moveFile(Long userId, Long fileId, Long targetDirId);

    boolean moveToRecycleBin(Long userId, Long fileId);

    boolean encryptFile(Long userId, Long fileId, String privatePassword);

    boolean decryptFile(Long userId, Long fileId, String privatePassword);
}
