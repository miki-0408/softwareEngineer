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
import org.example.netdisk.Service.Support.FileConflictException;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Service.Support.TransformService;
import org.example.netdisk.Utils.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
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
    public R_File uploadFiles(Long userId, Long dirId, List<MultipartFile> files, List<String> relativePaths,
                              boolean encrypt, String privatePassword, String packMethod, String compressMethod,
                              String displayName) {
        if (files == null || files.isEmpty()) throw new RuntimeException("上传文件不能为空");
        Long targetDirId = resolveEncryptTarget(userId, dirId, encrypt, privatePassword);
        int isEncryptedFlag = encrypt ? encrypted : notEncrypted;
        boolean actuallyTarred = files.size() > 1 || "tar".equals(packMethod);

        try {
            byte[] rawBytes;
            if (files.size() == 1 && !"tar".equals(packMethod)) {
                rawBytes = files.get(0).getBytes();
            } else {
                List<TarUtil.TarEntry> entries = new ArrayList<>();
                for (int i = 0; i < files.size(); i++) {
                    MultipartFile f = files.get(i);
                    String entryName = (relativePaths != null && i < relativePaths.size() && relativePaths.get(i) != null)
                        ? relativePaths.get(i).replace('\\', '/')
                        : f.getOriginalFilename() != null ? f.getOriginalFilename() : "file" + i;
                    if (entryName.isEmpty()) entryName = "file" + i;
                    entries.add(new TarUtil.TarEntry(entryName, f.getBytes(), false));
                }
                rawBytes = TarUtil.createTar(entries);
                packMethod = "none";
            }

            String finalName;
            if (displayName != null && !displayName.isBlank()) {
                finalName = displayName;
            } else if (files.size() == 1 && !actuallyTarred) {
                finalName = Objects.requireNonNullElse(files.get(0).getOriginalFilename(), "未命名文件");
            } else {
                finalName = files.size() == 1
                    ? Objects.requireNonNullElse(files.get(0).getOriginalFilename(), "file") + ".tar"
                    : "archive_" + System.currentTimeMillis() + ".tar";
            }

            byte[] processed = processUpload(rawBytes, packMethod, compressMethod, encrypt ? privatePassword : null);
            StorageSpace sp = requireStorage(userId, processed.length);
            return transformService.transformFileToRFile(
                insertAndSaveFile(userId, targetDirId, finalName, processed.length, processed, compressMethod, isEncryptedFlag, sp));
        } catch (IOException e) { throw new RuntimeException("文件上传失败", e); }
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
    public boolean renameFile(Long userId, Long fileId, String newFileName, boolean force) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) return false;
        if (!newFileName.equals(netdiskFile.getFileName())) {
            resolveConflict(userId, netdiskFile.getDirId(), newFileName, force);
        }
        netdiskFile.setFileName(newFileName);
        netdiskFile.setFileType(FileUtils.extractExtension(newFileName));
        return fileMapper.updateFile(netdiskFile) > 0;
    }

    @Override
    public boolean moveFile(Long userId, Long fileId, Long targetDirId, boolean force) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) return false;
        Directory targetDir = directoryMapper.selectDirectoryById(targetDirId, userId);
        if (targetDir == null) return false;
        if (!targetDirId.equals(netdiskFile.getDirId())) {
            resolveConflict(userId, targetDirId, netdiskFile.getFileName(), force);
        }
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

    // ==================== 移入/移出私密空间 ====================

    @Override
    public boolean encryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId, boolean force) {
        return recompressFile(userId, fileId, privatePassword, targetDirId, force, encrypted);
    }

    @Override
    public boolean decryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId, boolean force) {
        return recompressFile(userId, fileId, privatePassword, targetDirId, force, notEncrypted);
    }

    /** 加密/解密存储字节（不解压不重压，只加/去加密层）：encryptFile 和 decryptFile 的公共实现 */
    private boolean recompressFile(Long userId, Long fileId, String password,
            Long targetDirId, boolean force, int targetEncryptedFlag) {
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId);
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) return false;
        boolean currentlyEncrypted = netdiskFile.getIsEncrypted() == encrypted;
        if (currentlyEncrypted == (targetEncryptedFlag == encrypted)) return true; // 已是目标状态
        privateSpaceService.validatePrivatePassword(userId, password);
        resolveConflict(userId, targetDirId, netdiskFile.getFileName(), force);

        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath());
        // 只加/去加密层，不触碰压缩层——避免无意义的解压再压缩往返
        byte[] newStoredBytes = (targetEncryptedFlag == encrypted)
            ? EncryptionUtil.encrypt(storedBytes, password)
            : EncryptionUtil.decrypt(storedBytes, password);
        fileStorageService.deleteStoredFile(netdiskFile.getPath());
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId());
        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length);

        netdiskFile.setPath(newPath);
        netdiskFile.setFileSize((long) newStoredBytes.length);
        netdiskFile.setDirId(targetDirId);
        netdiskFile.setIsEncrypted(targetEncryptedFlag);
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

    /** 检查同名文件，不存在返回 true；存在且 force 则删除旧文件返回 true；存在且 !force 则抛异常 */
    private boolean resolveConflict(Long userId, Long dirId, String fileName, boolean force) {
        if (fileMapper.countFilesByName(userId, dirId, fileName) > 0) {
            if (!force) throw new FileConflictException(fileName);
            // 删除同名旧文件（不限 isEncrypted，覆盖普通和私密空间）
            List<NetdiskFile> files = fileMapper.selectFilesByNameAndDir(userId, dirId, fileName, fileStatusNormal);
            for (NetdiskFile f : files) {
                if (fileName.equals(f.getFileName())) {
                    fileStorageService.deleteStoredFile(f.getPath());
                    releaseStorage(userId, f.getFileSize());
                    fileMapper.deleteFile(f.getFileId(), userId);
                    break;
                }
            }
        }
        return true;
    }

    /** 保留旧逻辑的兜底：冲突时自动加后缀（用于上传等场景） */
    public String uniqueFileName(Long userId, Long dirId, String fileName) {
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

    /** 加密上传时解析目标目录 */
    private Long resolveEncryptTarget(Long userId, Long dirId, boolean encrypt, String privatePassword) {
        if (!encrypt) {
            Directory d = directoryMapper.selectDirectoryById(dirId, userId);
            if (d == null) throw new RuntimeException("目录不存在");
            return dirId;
        }
        privateSpaceService.validatePrivatePassword(userId, privatePassword);
        Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId);
        if (privateRoot == null) throw new RuntimeException("私密空间根目录不存在，请先启用私密空间");
        Long target = isInPrivateSpace(userId, dirId, privateRoot.getDirId()) ? dirId : privateRoot.getDirId();
        Directory d = directoryMapper.selectDirectoryById(target, userId);
        if (d == null) throw new RuntimeException("目录不存在");
        return target;
    }

    /** 检查并返回存储空间 */
    private StorageSpace requireStorage(Long userId, long size) {
        StorageSpace sp = storageSpaceMapper.selectByUserId(userId);
        if (sp == null || sp.getRemainSpace() < size) throw new RuntimeException("存储空间不足");
        return sp;
    }

    /** 插入文件记录 + 写磁盘 + 更新存储空间 */
    private NetdiskFile insertAndSaveFile(Long userId, Long targetDirId, String displayName,
            long storedSize, byte[] processed, String compressMethod, int isEncryptedFlag,
            StorageSpace storageSpace) throws IOException {
        displayName = uniqueFileName(userId, targetDirId, displayName);
        NetdiskFile f = new NetdiskFile();
        f.setFileName(displayName);
        f.setFileType(FileUtils.extractExtension(displayName));
        f.setFileSize(storedSize);
        f.setUserId(userId);
        f.setDirId(targetDirId);
        f.setStatus(fileStatusNormal);
        f.setIsEncrypted(isEncryptedFlag);
        f.setCompressMethod((int) compressCode(compressMethod));
        fileMapper.insertFile(f);
        f.setPath(fileStorageService.saveCompressedFile(processed, userId, f.getFileId()));
        fileMapper.updateFile(f);
        updateStorageUsed(storageSpace, storedSize);
        return f;
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

}
