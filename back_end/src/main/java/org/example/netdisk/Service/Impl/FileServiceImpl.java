package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.Directory;
import org.example.netdisk.Entity.NetdiskFile;
import org.example.netdisk.Entity.StorageSpace;
import org.example.netdisk.Mapper.DirectoryMapper;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.Mapper.StorageSpaceMapper;
import org.example.netdisk.ResponseDTO.R_Directory;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.Service.Inter.FileService;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Service.Support.TransformService;
import org.example.netdisk.Utils.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.example.netdisk.Service.Support.Enum.*;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private DirectoryMapper directoryMapper;
    @Autowired
    private StorageSpaceMapper storageSpaceMapper;
    @Autowired
    private PrivateSpaceServiceImpl privateSpaceService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private TransformService transformService;

    // ==================== 文件上传 ====================

    @Override
    public R_File uploadFile(Long userId, Long dirId, MultipartFile file, boolean encrypt,
                             String privatePassword, String packMethod, String compressMethod) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        Long targetDirId = dirId;
        int isEncryptedFlag = notEncrypted;

        if (encrypt) {
            privateSpaceService.validatePrivatePassword(userId, privatePassword);
            Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
            if (privateRoot == null) {
                throw new RuntimeException("私密空间根目录不存在，请先启用私密空间");
            }
            // 如果源目录已在私密空间内，保持原 dirId；否则重定向到私密空间根
            if (!isInPrivateSpace(userId, dirId, privateRoot.getDirId())) {
                targetDirId = privateRoot.getDirId();
            }
            isEncryptedFlag = encrypted;
        }
        Directory directory = directoryMapper.selectDirectoryById(targetDirId, userId);
        if (directory == null) {
            throw new RuntimeException("目录不存在");
        }

        try {
            byte[] rawBytes = file.getBytes();

            // 处理管道: 打包 → 压缩 → 加密
            byte[] processed = processUpload(rawBytes, packMethod, compressMethod,
                encrypt ? privatePassword : null);

            long storedSize = processed.length;

            StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
            if (storageSpace == null || storageSpace.getRemainSpace() < storedSize) {
                throw new RuntimeException("存储空间不足");
            }

            // 文件名加上打包后缀
            String displayName = Objects.requireNonNullElse(file.getOriginalFilename(), "未命名文件");
            if ("tar".equals(packMethod) && !displayName.endsWith(".tar")) {
                displayName += ".tar";
            }
            displayName = uniqueFileName(userId, targetDirId, displayName);

            NetdiskFile netdiskFile = new NetdiskFile();
            netdiskFile.setFileName(displayName);
            netdiskFile.setFileType(extractFileType(displayName));
            netdiskFile.setFileSize(storedSize);
            netdiskFile.setUserId(userId);
            netdiskFile.setDirId(targetDirId);
            netdiskFile.setStatus(fileStatusNormal);
            netdiskFile.setIsEncrypted(isEncryptedFlag);
            netdiskFile.setCompressMethod((int) compressCode(compressMethod));
            fileMapper.insertFile(netdiskFile);

            String path = fileStorageService.saveCompressedFile(processed, userId, netdiskFile.getFileId());
            netdiskFile.setPath(path);
            fileMapper.updateFile(netdiskFile);

            updateStorageUsed(storageSpace, storedSize);
            return transformService.transformFileToRFile(netdiskFile);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    // ==================== 多文件/文件夹上传 ====================

    @Override
    public R_File uploadFiles(Long userId, Long dirId, List<MultipartFile> files, List<String> relativePaths,
                              boolean encrypt, String privatePassword, String packMethod, String compressMethod,
                              String displayName) {
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        Long targetDirId = dirId;
        int isEncryptedFlag = notEncrypted;

        if (encrypt) {
            privateSpaceService.validatePrivatePassword(userId, privatePassword);
            Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
            if (privateRoot == null) {
                throw new RuntimeException("私密空间根目录不存在，请先启用私密空间");
            }
            // 如果源目录已在私密空间内，保持原 dirId；否则重定向到私密空间根
            if (!isInPrivateSpace(userId, dirId, privateRoot.getDirId())) {
                targetDirId = privateRoot.getDirId();
            }
            isEncryptedFlag = encrypted;
        }
        Directory directory = directoryMapper.selectDirectoryById(targetDirId, userId);
        if (directory == null) {
            throw new RuntimeException("目录不存在");
        }

        try {
            byte[] rawBytes;
            boolean actuallyTarred = files.size() > 1 || "tar".equals(packMethod);

            if (files.size() == 1 && !"tar".equals(packMethod)) {
                // 单文件、不打包：直接用文件内容
                rawBytes = files.get(0).getBytes();
            } else {
                // 多文件 或 要求打包：构建 tar 归档
                List<TarUtil.TarEntry> entries = new java.util.ArrayList<>();
                for (int i = 0; i < files.size(); i++) {
                    MultipartFile f = files.get(i);
                    String entryName;
                    if (relativePaths != null && i < relativePaths.size() && relativePaths.get(i) != null) {
                        entryName = relativePaths.get(i).replace('\\', '/');
                    } else {
                        entryName = f.getOriginalFilename() != null ? f.getOriginalFilename() : "file" + i;
                    }
                    if (entryName.isEmpty()) entryName = "file" + i;
                    entries.add(new TarUtil.TarEntry(entryName, f.getBytes(), false));
                }
                rawBytes = TarUtil.createTar(entries);
                packMethod = "none"; // 已在外面打好 tar，processUpload 不要再打一层
            }

            // 生成显示名称（前端传来的优先）
            String finalName;
            if (displayName != null && !displayName.isBlank()) {
                finalName = displayName;
            } else if (files.size() == 1 && !actuallyTarred) {
                finalName = Objects.requireNonNullElse(files.get(0).getOriginalFilename(), "未命名文件");
            } else if (files.size() == 1) {
                finalName = Objects.requireNonNullElse(files.get(0).getOriginalFilename(), "file");
                if (!finalName.endsWith(".tar")) finalName += ".tar";
            } else {
                finalName = "archive_" + System.currentTimeMillis() + ".tar";
            }
            finalName = uniqueFileName(userId, targetDirId, finalName);

            byte[] processed = processUpload(rawBytes, packMethod, compressMethod,
                encrypt ? privatePassword : null);

            long storedSize = processed.length;

            StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
            if (storageSpace == null || storageSpace.getRemainSpace() < storedSize) {
                throw new RuntimeException("存储空间不足");
            }

            NetdiskFile netdiskFile = new NetdiskFile();
            netdiskFile.setFileName(finalName);
            netdiskFile.setFileType(extractFileType(finalName));
            netdiskFile.setFileSize(storedSize);
            netdiskFile.setUserId(userId);
            netdiskFile.setDirId(targetDirId);
            netdiskFile.setStatus(fileStatusNormal);
            netdiskFile.setIsEncrypted(isEncryptedFlag);
            netdiskFile.setCompressMethod((int) compressCode(compressMethod));
            fileMapper.insertFile(netdiskFile);

            String path = fileStorageService.saveCompressedFile(processed, userId, netdiskFile.getFileId());
            netdiskFile.setPath(path);
            fileMapper.updateFile(netdiskFile);

            updateStorageUsed(storageSpace, storedSize);
            return transformService.transformFileToRFile(netdiskFile);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    // ==================== 文件下载 ====================

    @Override
    public byte[] downloadFile(Long userId, Long fileId, String privatePassword) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            throw new RuntimeException("文件不存在");
        }
        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        return processDownload(storedBytes,
            netdiskFile.getIsEncrypted() == encrypted ? privatePassword : null,
            netdiskFile.getCompressMethod());
    }

    // ==================== 处理流水线 ====================

    private static final byte COMP_LZ77 = 1, COMP_HUFFMAN = 2;

    private byte compressCode(String method) {
        return "huffman".equals(method) ? COMP_HUFFMAN : COMP_LZ77;
    }

    /** 打包 → 压缩 → 加密 */
    private byte[] processUpload(byte[] raw, String packMethod, String compressMethod, String password) {
        if ("tar".equals(packMethod)) {
            try {
                TarUtil.TarEntry entry = new TarUtil.TarEntry("file", raw, false);
                raw = TarUtil.createTar(List.of(entry));
            } catch (IOException e) {
                throw new RuntimeException("打包失败", e);
            }
        }
        if ("huffman".equals(compressMethod)) raw = HuffmanCompression.compress(raw);
        else raw = LZ77Compression.compress(raw);

        if (password != null) raw = EncryptionUtil.encrypt(raw, password);
        return raw;
    }

    /** 解密 → 解压 */
    private byte[] processDownload(byte[] data, String password, Integer compressMethod) {
        if (password != null) data = EncryptionUtil.decrypt(data, password);

        byte comp = (compressMethod != null) ? compressMethod.byteValue() : COMP_LZ77;
        return (comp == COMP_HUFFMAN) ? HuffmanCompression.decompress(data) : LZ77Compression.decompress(data);
    }

    // ==================== 普通文件列表（自动排除已加密文件） ====================

    @Override
    public List<R_File> listFiles(Long userId, Long dirId) {
        Directory directory = directoryMapper.selectDirectoryById(dirId, userId);
        if (directory == null) {
            throw new RuntimeException("目录不存在");
        }
        List<NetdiskFile> files = fileMapper.selectFilesByDirAndStatus(userId, dirId, fileStatusNormal);
        List<R_File> result = new ArrayList<>();
        for (NetdiskFile file : files) {
            result.add(transformService.transformFileToRFile(file));
        }
        return result;
    }

    // ==================== 私密空间文件列表 ====================

    public List<R_File> listPrivateFiles(Long userId, Long dirId) {
        List<NetdiskFile> files = fileMapper.selectPrivateFilesByDirAndStatus(userId, dirId, fileStatusNormal);
        List<R_File> result = new ArrayList<>();
        for (NetdiskFile file : files) {
            result.add(transformService.transformFileToRFile(file));
        }
        return result;
    }

    // ==================== 私密空间目录列表 ====================

    public List<R_Directory> listPrivateDirectories(Long userId, Long parentDirId) {
        List<Directory> directories;
        if (parentDirId == null) {
            // 返回私密空间根目录
            Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
            if (privateRoot == null) return List.of();
            return List.of(transformService.transformDirectoryToRDirectory(privateRoot));
        }
        directories = directoryMapper.selectDirectoriesByParentId(userId, parentDirId);
        List<R_Directory> result = new ArrayList<>();
        for (Directory d : directories) {
            result.add(transformService.transformDirectoryToRDirectory(d));
        }
        return result;
    }

    // ==================== 重命名 / 移动 / 删除 ====================

    @Override
    public boolean renameFile(Long userId, Long fileId, String newFileName) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        newFileName = uniqueFileName(userId, netdiskFile.getDirId(), newFileName);
        netdiskFile.setFileName(newFileName);
        netdiskFile.setFileType(extractFileType(newFileName));
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    @Override
    public boolean moveFile(Long userId, Long fileId, Long targetDirId) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        Directory targetDir = directoryMapper.selectDirectoryById(targetDirId, userId);
        if (targetDir == null) {
            return false;
        }
        netdiskFile.setFileName(uniqueFileName(userId, targetDirId, netdiskFile.getFileName()));
        netdiskFile.setDirId(targetDirId);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    @Override
    public boolean moveToRecycleBin(Long userId, Long fileId) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        return fileMapper.updateFileStatus(fileId, userId, fileStatusRecycle) > 0;
    }

    // ==================== 移入私密空间（加密 + 移动到私密根目录） ====================

    @Override
    public boolean encryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) return false;
        if (netdiskFile.getIsEncrypted() == encrypted) return true;
        privateSpaceService.validatePrivatePassword(userId, privatePassword);

        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        byte[] rawBytes = processDownload(storedBytes, null, netdiskFile.getCompressMethod());
        byte[] newStoredBytes = processUpload(rawBytes, "none", "lz77", privatePassword);
        netdiskFile.setCompressMethod((int) COMP_LZ77);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());
        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);

        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);
        netdiskFile.setFileName(uniqueFileName(userId, targetDirId, netdiskFile.getFileName()));
        netdiskFile.setDirId(targetDirId);
        netdiskFile.setIsEncrypted(encrypted);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    // ==================== 移出私密空间 ====================

    @Override
    public boolean decryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) return false;
        if (netdiskFile.getIsEncrypted() != encrypted) return true;
        privateSpaceService.validatePrivatePassword(userId, privatePassword);

        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        byte[] rawBytes = processDownload(storedBytes, privatePassword, netdiskFile.getCompressMethod());
        byte[] newStoredBytes = processUpload(rawBytes, "none", "lz77", null);
        netdiskFile.setCompressMethod((int) COMP_LZ77);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());
        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);

        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);
        netdiskFile.setFileName(uniqueFileName(userId, targetDirId, netdiskFile.getFileName()));
        netdiskFile.setDirId(targetDirId);
        netdiskFile.setIsEncrypted(notEncrypted);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    // ==================== 工具方法 ====================

    private void updateStorageUsed(StorageSpace storageSpace, long delta) {
        storageSpace.setUsedSpace(storageSpace.getUsedSpace() + delta);
        storageSpace.setRemainSpace(storageSpace.getTotalSpace() - storageSpace.getUsedSpace());
        storageSpaceMapper.updateStorageSpace(storageSpace);
    }

    private void adjustStorageOnSizeChange(Long userId, Long oldSize, long newSize) {
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
        if (storageSpace == null) return;
        long delta = newSize - oldSize;
        if (storageSpace.getRemainSpace() < delta) {
            throw new RuntimeException("存储空间不足");
        }
        updateStorageUsed(storageSpace, delta);
    }

    void releaseStorage(Long userId, Long fileSize) {
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId);
        if (storageSpace == null || fileSize == null) return;
        storageSpace.setUsedSpace(Math.max(0, storageSpace.getUsedSpace() - fileSize));
        storageSpace.setRemainSpace(storageSpace.getTotalSpace() - storageSpace.getUsedSpace());
        storageSpaceMapper.updateStorageSpace(storageSpace);
    }

    /** 确保文件名在目录中唯一，冲突时加 (1)(2) 后缀 */
    private String uniqueFileName(Long userId, Long dirId, String fileName) {
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }
        String candidate = fileName;
        int n = 1;
        while (fileMapper.countFilesByName(userId, dirId, candidate) > 0) {
            candidate = base + " (" + n + ")" + ext;
            n++;
        }
        return candidate;
    }

    /** 检查 dirId 是否是 ancestorDirId 的后代（含自身） */
    private boolean isUnderDir(Long userId, Long dirId, Long ancestorDirId) {
        if (dirId == null || ancestorDirId == null) return false;
        if (dirId.equals(ancestorDirId)) return true;
        Long current = dirId;
        for (int i = 0; i < 20; i++) { // 最多向上查 20 层
            Directory d = directoryMapper.selectDirectoryById(current, userId);
            if (d == null) return false;
            if (d.getParentDirId() == null) return false;
            if (d.getParentDirId().equals(ancestorDirId)) return true;
            current = d.getParentDirId();
        }
        return false;
    }

    /** 检查 dirId 是否在 privateRootId 的子树内（含自身） */
    private boolean isInPrivateSpace(Long userId, Long dirId, Long privateRootId) {
        if (dirId == null || privateRootId == null) return false;
        if (dirId.equals(privateRootId)) return true;
        Long cur = dirId;
        for (int i = 0; i < 20; i++) {
            Directory d = directoryMapper.selectDirectoryById(cur, userId);
            if (d == null || d.getParentDirId() == null) return false;
            if (d.getParentDirId().equals(privateRootId)) return true;
            cur = d.getParentDirId();
        }
        return false;
    }

    private String extractFileType(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1).toLowerCase();
    }
}
