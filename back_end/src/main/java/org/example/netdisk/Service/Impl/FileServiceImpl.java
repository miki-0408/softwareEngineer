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
        Long originalDirId = null;

        if (encrypt) {
            privateSpaceService.validatePrivatePassword(userId, privatePassword);
            Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
            if (privateRoot == null) {
                throw new RuntimeException("私密空间根目录不存在，请先启用私密空间");
            }
            targetDirId = privateRoot.getDirId();
            isEncryptedFlag = encrypted;
            originalDirId = dirId;
        } else {
            Directory directory = directoryMapper.selectDirectoryById(dirId, userId);
            if (directory == null) {
                throw new RuntimeException("目录不存在");
            }
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
            netdiskFile.setOriginalDirId(originalDirId);
            netdiskFile.setStatus(fileStatusNormal);
            netdiskFile.setIsEncrypted(isEncryptedFlag);
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
        Long originalDirId = null;

        if (encrypt) {
            privateSpaceService.validatePrivatePassword(userId, privatePassword);
            Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
            if (privateRoot == null) {
                throw new RuntimeException("私密空间根目录不存在，请先启用私密空间");
            }
            targetDirId = privateRoot.getDirId();
            isEncryptedFlag = encrypted;
            originalDirId = dirId;
        } else {
            Directory directory = directoryMapper.selectDirectoryById(dirId, userId);
            if (directory == null) {
                throw new RuntimeException("目录不存在");
            }
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
            netdiskFile.setOriginalDirId(originalDirId);
            netdiskFile.setStatus(fileStatusNormal);
            netdiskFile.setIsEncrypted(isEncryptedFlag);
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
            netdiskFile.getIsEncrypted() == encrypted ? privatePassword : null);
    }

    // ==================== 处理流水线 ====================

    private static final byte[] MAGIC = { 'N', 'D', 'K', 'K' };
    private static final byte PACK_NONE = 0, PACK_TAR = 1;
    private static final byte COMP_LZ77 = 1, COMP_HUFFMAN = 2;

    /** 解压 → 解密（用于读取私密空间文件，与 encryptFile 的 加密→压缩 对应） */
    private byte[] decompressThenDecrypt(byte[] data, byte compCode, String password) {
        byte[] body;
        boolean hasHeader = data.length >= 6 && data[0] == MAGIC[0] && data[1] == MAGIC[1]
            && data[2] == MAGIC[2] && data[3] == MAGIC[3];
        if (hasHeader) {
            body = new byte[data.length - 6];
            System.arraycopy(data, 6, body, 0, body.length);
        } else {
            body = data;
            compCode = COMP_LZ77;
        }
        if (compCode == COMP_HUFFMAN) body = HuffmanCompression.decompress(body);
        else body = LZ77Compression.decompress(body);
        if (password != null) body = EncryptionUtil.decrypt(body, password);
        return body;
    }

    /** 加密 → 压缩 → 加头（用于移入私密空间，解密时对应 decompressThenDecrypt） */
    private byte[] encryptThenCompress(byte[] data, byte compCode, String password) {
        if (password != null) data = EncryptionUtil.encrypt(data, password);
        if (compCode == COMP_HUFFMAN) data = HuffmanCompression.compress(data);
        else data = LZ77Compression.compress(data);
        byte[] result = new byte[6 + data.length];
        System.arraycopy(MAGIC, 0, result, 0, 4);
        result[4] = PACK_NONE;
        result[5] = compCode;
        System.arraycopy(data, 0, result, 6, data.length);
        return result;
    }

    /** 上传管道: 打包 → 压缩 → 加密 → 加头 */
    private byte[] processUpload(byte[] raw, String packMethod, String compressMethod, String password) {
        byte packCode = "tar".equals(packMethod) ? PACK_TAR : PACK_NONE;
        byte compCode = "huffman".equals(compressMethod) ? COMP_HUFFMAN : COMP_LZ77;

        // 1. 打包
        if (packCode == PACK_TAR) {
            try {
                TarUtil.TarEntry entry = new TarUtil.TarEntry("file", raw, false);
                raw = TarUtil.createTar(List.of(entry));
            } catch (IOException e) {
                throw new RuntimeException("打包失败", e);
            }
        }

        // 2. 压缩
        if (compCode == COMP_HUFFMAN) {
            raw = HuffmanCompression.compress(raw);
        } else {
            raw = LZ77Compression.compress(raw);
        }

        // 3. 加密
        if (password != null) {
            raw = EncryptionUtil.encrypt(raw, password);
        }

        // 4. 加头
        byte[] result = new byte[6 + raw.length];
        System.arraycopy(MAGIC, 0, result, 0, 4);
        result[4] = packCode;
        result[5] = compCode;
        System.arraycopy(raw, 0, result, 6, raw.length);
        return result;
    }

    /** 下载管道: 去头 → 解密 → 解压 → 解包 */
    private byte[] processDownload(byte[] data, String password) {
        // 0. 兼容旧文件（无 "NDKK" 头）
        boolean legacy = data.length < 6
            || data[0] != MAGIC[0] || data[1] != MAGIC[1]
            || data[2] != MAGIC[2] || data[3] != MAGIC[3];

        byte packCode = PACK_NONE;
        byte compCode = COMP_LZ77;  // 旧文件默认 LZ77

        if (!legacy) {
            packCode = data[4];
            compCode = data[5];
            byte[] stripped = new byte[data.length - 6];
            System.arraycopy(data, 6, stripped, 0, stripped.length);
            data = stripped;
        }

        // 1. 解密
        if (password != null) {
            data = EncryptionUtil.decrypt(data, password);
        }

        // 2. 解压
        if (compCode == COMP_HUFFMAN) {
            data = HuffmanCompression.decompress(data);
        } else {
            data = LZ77Compression.decompress(data);
        }

        // 3. 不解包：tar 文件直接返回，用户自行用工具解包
        return data;
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
    public boolean encryptFile(Long userId, Long fileId, String privatePassword) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        if (netdiskFile.getIsEncrypted() == encrypted) {
            return true;
        }
        privateSpaceService.validatePrivatePassword(userId, privatePassword);
        Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
        if (privateRoot == null) {
            throw new RuntimeException("私密空间根目录不存在");
        }

        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        // processDownload 解压得到原文 → processUpload 加密+压缩（统一外层加密格式）
        byte[] rawBytes = processDownload(storedBytes, null);
        byte[] newStoredBytes = processUpload(rawBytes, "none", "lz77", privatePassword);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());

        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);

        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);
        netdiskFile.setOriginalDirId(netdiskFile.getDirId());  // 记住来源目录
        netdiskFile.setDirId(privateRoot.getDirId());          // 移到私密空间
        netdiskFile.setIsEncrypted(encrypted);
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    // ==================== 移出私密空间（解密 + 恢复到原目录） ====================

    @Override
    public boolean decryptFile(Long userId, Long fileId, String privatePassword) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) {
            return false;
        }
        if (netdiskFile.getIsEncrypted() != encrypted) {
            return true;
        }
        privateSpaceService.validatePrivatePassword(userId, privatePassword);

        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        byte[] rawBytes = processDownload(storedBytes, privatePassword);
        byte[] newStoredBytes = processUpload(rawBytes, "none", "lz77", null);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());

        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);

        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);

        // 恢复目标目录：优先用原始目录；如果已删除/为空/属于私密空间内部 → 恢复到"我的文件"
        Long originalDir = netdiskFile.getOriginalDirId();
        Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
        Directory targetDir = (originalDir != null) ? directoryMapper.selectDirectoryById(originalDir, userId) : null;
        if (targetDir == null || (privateRoot != null && originalDir.equals(privateRoot.getDirId()))) {
            // 原始目录已被删除 / 从私密空间直接上传的文件 → 恢复到用户根目录
            Directory userRoot = directoryMapper.selectRootDirectory(userId);
            originalDir = userRoot != null ? userRoot.getDirId() : null;
        }
        netdiskFile.setFileName(uniqueFileName(userId, originalDir, netdiskFile.getFileName()));
        netdiskFile.setDirId(originalDir);
        netdiskFile.setOriginalDirId(null);
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

    private String extractFileType(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1).toLowerCase();
    }
}
