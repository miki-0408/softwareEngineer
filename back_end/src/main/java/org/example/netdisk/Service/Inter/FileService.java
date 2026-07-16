package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_File;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService { // 文件服务接口：定义文件的上传、下载、管理等核心操作

    R_File uploadFiles(Long userId, Long dirId, List<MultipartFile> files, List<String> relativePaths, // 上传文件：支持多文件、加密、打包、压缩等选项
                       boolean encrypt, String privatePassword, String packMethod, String compressMethod,
                       String displayName);

    byte[] downloadFile(Long userId, Long fileId, String privatePassword); // 下载文件原始字节数据

    List<R_File> listFiles(Long userId, Long dirId); // 列出指定目录下的文件列表

    boolean renameFile(Long userId, Long fileId, String newFileName, boolean force); // 重命名文件，force=true时强制覆盖同名

    boolean moveFile(Long userId, Long fileId, Long targetDirId, boolean force); // 移动文件到目标目录，force=true时强制覆盖

    boolean moveToRecycleBin(Long userId, Long fileId); // 将文件移入回收站（软删除）

    boolean encryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId, boolean force); // 加密文件（移入私密空间）

    boolean decryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId, boolean force); // 解密文件（移出私密空间）

    String uniqueFileName(Long userId, Long dirId, String fileName); // 生成唯一文件名（重名时追加编号）
}
