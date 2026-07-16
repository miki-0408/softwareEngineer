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
    private FileMapper fileMapper; // 文件表数据库操作接口
    @Autowired
    private DirectoryMapper directoryMapper; // 目录表数据库操作接口，用于校验目录存在性
    @Autowired
    private StorageSpaceMapper storageSpaceMapper; // 存储空间表数据库操作接口
    @Autowired
    private PrivateSpaceServiceImpl privateSpaceService; // 私密空间服务，用于密码校验和根目录查找
    @Autowired
    private FileStorageService fileStorageService; // 物理文件存储服务，负责磁盘读写
    @Autowired
    private TransformService transformService; // 实体与响应DTO之间的转换服务

    @Override
    public R_File uploadFiles(Long userId, Long dirId, List<MultipartFile> files, List<String> relativePaths,
                              boolean encrypt, String privatePassword, String packMethod, String compressMethod,
                              String displayName) { // 上传文件流程：校验参数→解析目标目录→打包→压缩→加密→存储→返回结果
        if (files == null || files.isEmpty()) throw new RuntimeException("上传文件不能为空"); // 文件列表为空则拒绝上传，保证参数有效性
        Long targetDirId = resolveEncryptTarget(userId, dirId, encrypt, privatePassword); // 根据加密标志解析最终目标目录ID
        int isEncryptedFlag = encrypt ? encrypted : notEncrypted; // 将布尔加密标志转为数据库存储的整型标志
        boolean actuallyTarred = files.size() > 1 || "tar".equals(packMethod); // 判断是否实际执行了tar打包（多文件或明确指定tar打包）

        try {
            byte[] rawBytes; // 存储打包后的原始字节数据
            if (files.size() == 1 && !"tar".equals(packMethod)) { // 单文件且不要求tar打包，直接读取文件字节
                rawBytes = files.get(0).getBytes(); // 从MultipartFile中读取原始字节
            } else { // 多文件或指定tar打包，需要先打包为tar格式
                List<TarUtil.TarEntry> entries = new ArrayList<>(); // 创建tar条目列表
                for (int i = 0; i < files.size(); i++) { // 遍历所有上传文件，构造tar条目
                    MultipartFile f = files.get(i); // 获取当前文件
                    String entryName = (relativePaths != null && i < relativePaths.size() && relativePaths.get(i) != null) // 优先使用前端传入的相对路径作为tar条目名
                        ? relativePaths.get(i).replace('\\', '\\') // 统一路径分隔符为Unix风格
                        : f.getOriginalFilename() != null ? f.getOriginalFilename() : "file" + i; // 降级使用原始文件名或默认名
                    if (entryName.isEmpty()) entryName = "file" + i; // 条目名为空时使用默认名称
                    entries.add(new TarUtil.TarEntry(entryName, f.getBytes(), false)); // 创建tar条目（false表示普通文件而非目录）
                }
                rawBytes = TarUtil.createTar(entries); // 将所有条目打包为tar格式字节数组
                packMethod = "none"; // tar打包完成，后续处理不再重复打包，置为none
            }

            String finalName; // 文件在系统中的最终显示名称
            if (displayName != null && !displayName.isBlank()) { // 前端明确指定了显示名称
                finalName = displayName; // 直接使用前端传入的显示名称
            } else if (files.size() == 1 && !actuallyTarred) { // 单文件且未执行tar打包，使用原始文件名
                finalName = Objects.requireNonNullElse(files.get(0).getOriginalFilename(), "未命名文件"); // 获取原始文件名，为null时使用默认名
            } else { // 多文件或已执行tar打包，生成tar包名称
                finalName = files.size() == 1
                    ? Objects.requireNonNullElse(files.get(0).getOriginalFilename(), "file") + ".tar" // 单文件被强制打包，原始文件名加.tar后缀
                    : "archive_" + System.currentTimeMillis() + ".tar"; // 多文件归档，使用时间戳生成唯一包名
            }

            byte[] processed = processUpload(rawBytes, packMethod, compressMethod, encrypt ? privatePassword : null); // 执行打包→压缩→加密处理流水线
            StorageSpace sp = requireStorage(userId, processed.length); // 检查用户存储空间是否足够
            return transformService.transformFileToRFile( // 将文件实体转为响应DTO返回
                insertAndSaveFile(userId, targetDirId, finalName, processed.length, processed, compressMethod, isEncryptedFlag, sp)); // 插入文件记录+写磁盘+更新存储
        } catch (IOException e) { throw new RuntimeException("文件上传失败", e); } // 捕获IO异常并包装为运行时异常抛出
    }

    @Override
    public byte[] downloadFile(Long userId, Long fileId, String privatePassword) { // 下载文件流程：查记录→读磁盘→解密→解压→返回原始字节
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId); // 查询文件记录并校验归属用户
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) { // 文件不存在或已删除/回收
            throw new RuntimeException("文件不存在"); // 拒绝下载
        }
        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath()); // 从磁盘读取物理文件字节
        return processDownload(storedBytes, // 执行解密→解压处理流水线
            netdiskFile.getIsEncrypted() == encrypted ? privatePassword : null, // 加密文件需要传入密码，普通文件传null
            netdiskFile.getCompressMethod()); // 传入压缩算法标识以选择正确的解压方式
    }

    private static final byte COMP_LZ77 = 1, COMP_HUFFMAN = 2; // 压缩算法常量：1=LZ77，2=Huffman，用于数据库存储和算法选择

    private byte compressCode(String method) { // 将压缩方法字符串转为数据库存储的字节编码
        return "huffman".equals(method) ? COMP_HUFFMAN : COMP_LZ77; // huffman用编码2，其余（包括lz77和默认）用编码1
    }

    private byte[] processUpload(byte[] raw, String packMethod, String compressMethod, String password) { // 上传处理流水线：tar打包→压缩→加密
        if ("tar".equals(packMethod)) { // 需要单独打包（单文件上传但指定了tar打包方式）
            try {
                TarUtil.TarEntry entry = new TarUtil.TarEntry("file", raw, false); // 创建单条目tar包
                raw = TarUtil.createTar(List.of(entry)); // 打包为tar格式
            } catch (IOException e) { // 打包过程发生IO异常
                throw new RuntimeException("打包失败", e); // 包装为运行时异常向上层传递
            }
        }
        if ("huffman".equals(compressMethod)) raw = HuffmanCompression.compress(raw); // 使用Huffman算法压缩数据
        else raw = LZ77Compression.compress(raw); // 默认使用LZ77算法压缩数据

        if (password != null) raw = EncryptionUtil.encrypt(raw, password); // 密码非空说明需要加密，使用密码对压缩后数据加密
        return raw; // 返回处理后的字节数组
    }

    private byte[] processDownload(byte[] data, String password, Integer compressMethod) { // 下载处理流水线：解密→解压
        if (password != null) data = EncryptionUtil.decrypt(data, password); // 密码非空说明文件已加密，先用密码解密

        byte comp = (compressMethod != null) ? compressMethod.byteValue() : COMP_LZ77; // 获取压缩算法编码，null默认LZ77
        return (comp == COMP_HUFFMAN) ? HuffmanCompression.decompress(data) : LZ77Compression.decompress(data); // 根据编码选择对应解压算法
    }

    @Override
    public List<R_File> listFiles(Long userId, Long dirId) { // 列出指定目录下的所有普通区文件
        Directory directory = directoryMapper.selectDirectoryById(dirId, userId); // 查询目录并校验归属用户
        if (directory == null) { // 目录不存在或不属于当前用户
            throw new RuntimeException("目录不存在"); // 抛出业务异常
        }
        List<NetdiskFile> files = fileMapper.selectFilesByDirAndStatus(userId, dirId, fileStatusNormal); // 查询该目录下状态为正常的文件
        List<R_File> result = new ArrayList<>(); // 创建响应DTO列表
        for (NetdiskFile file : files) { // 遍历所有文件实体
            result.add(transformService.transformFileToRFile(file)); // 逐个转换为响应DTO并加入结果集
        }
        return result; // 返回文件列表
    }

    public List<R_File> listPrivateFiles(Long userId, Long dirId) { // 列出指定目录下的所有私密空间文件（含加密文件）
        List<NetdiskFile> files = fileMapper.selectPrivateFilesByDirAndStatus(userId, dirId, fileStatusNormal); // 查询私密空间文件（不过滤加密标志）
        List<R_File> result = new ArrayList<>(); // 创建响应DTO列表
        for (NetdiskFile file : files) { // 遍历所有文件实体
            result.add(transformService.transformFileToRFile(file)); // 逐个转换为响应DTO并加入结果集
        }
        return result; // 返回私密文件列表
    }

    public List<R_Directory> listPrivateDirectories(Long userId, Long parentDirId) { // 列出私密空间目录结构
        List<Directory> directories; // 存储查询结果
        if (parentDirId == null) { // parentDirId为空，返回私密空间根目录
            Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId); // 查找用户的私密空间根目录
            if (privateRoot == null) return List.of(); // 未找到私密空间根目录，返回空列表
            return List.of(transformService.transformDirectoryToRDirectory(privateRoot)); // 仅返回私密空间根目录
        }
        directories = directoryMapper.selectDirectoriesByParentId(userId, parentDirId); // 按父目录ID查询子目录列表
        List<R_Directory> result = new ArrayList<>(); // 创建响应DTO列表
        for (Directory d : directories) { // 遍历所有目录实体
            result.add(transformService.transformDirectoryToRDirectory(d)); // 逐个转换为响应DTO并加入结果集
        }
        return result; // 返回子目录列表
    }

    @Override
    public boolean renameFile(Long userId, Long fileId, String newFileName, boolean force) { // 重命名文件：查记录→冲突检查→更新名称和扩展名
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId); // 查询文件记录并校验归属用户
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) return false; // 文件不存在或状态异常，重命名失败
        if (!newFileName.equals(netdiskFile.getFileName())) { // 新名称与旧名称不同才需要检查冲突
            resolveConflict(userId, netdiskFile.getDirId(), newFileName, force); // 检查目标目录下是否存在同名文件，force模式会覆盖
        }
        netdiskFile.setFileName(newFileName); // 更新文件名为新名称
        netdiskFile.setFileType(FileUtils.extractExtension(newFileName)); // 根据新文件名自动提取文件扩展名
        return fileMapper.updateFile(netdiskFile) > 0; // 执行更新并返回是否成功
    }

    @Override
    public boolean moveFile(Long userId, Long fileId, Long targetDirId, boolean force) { // 移动文件到指定目录
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId); // 查询文件记录并校验归属用户
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) return false; // 文件不存在或状态异常，移动失败
        Directory targetDir = directoryMapper.selectDirectoryById(targetDirId, userId); // 查询目标目录并校验归属用户
        if (targetDir == null) return false; // 目标目录不存在，移动失败
        if (!targetDirId.equals(netdiskFile.getDirId())) { // 目标目录与当前目录不同才需要处理
            resolveConflict(userId, targetDirId, netdiskFile.getFileName(), force); // 检查目标目录是否存在同名文件
        }
        netdiskFile.setDirId(targetDirId); // 更新文件所属目录ID
        return fileMapper.updateFile(netdiskFile) > 0; // 执行更新并返回是否成功
    }

    @Override
    public boolean moveToRecycleBin(Long userId, Long fileId) { // 将文件移入回收站（软删除，仅改状态）
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId); // 查询文件记录并校验归属用户
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) { // 文件不存在或已回收/删除
            return false; // 移入回收站失败
        }
        return fileMapper.updateFileStatus(fileId, userId, fileStatusRecycle) > 0; // 将文件状态更新为回收站状态
    }

    @Override
    public boolean encryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId, boolean force) { // 将文件移入私密空间（加密存储）
        return recompressFile(userId, fileId, privatePassword, targetDirId, force, encrypted); // 委托公共方法，目标加密标志为已加密
    }

    @Override
    public boolean decryptFile(Long userId, Long fileId, String privatePassword, Long targetDirId, boolean force) { // 将文件移出私密空间（解密存储）
        return recompressFile(userId, fileId, privatePassword, targetDirId, force, notEncrypted); // 委托公共方法，目标加密标志为未加密
    }

    private boolean recompressFile(Long userId, Long fileId, String password,
            Long targetDirId, boolean force, int targetEncryptedFlag) { // 加解密文件存储的公共实现：校验→验证密码→读取→加密/解密→写盘→更新记录
        NetdiskFile netdiskFile = fileMapper.selectFileById(fileId, userId); // 查询文件记录并校验归属用户
        if (netdiskFile == null || netdiskFile.getStatus() != fileStatusNormal) return false; // 文件不存在或状态异常
        boolean currentlyEncrypted = netdiskFile.getIsEncrypted() == encrypted; // 判断文件当前是否已加密
        if (currentlyEncrypted == (targetEncryptedFlag == encrypted)) return true; // 当前状态与目标状态一致，无需操作，直接返回成功
        privateSpaceService.validatePrivatePassword(userId, password); // 验证用户私密空间密码，确保操作权限
        resolveConflict(userId, targetDirId, netdiskFile.getFileName(), force); // 检查目标目录是否存在同名文件冲突

        byte[] storedBytes = fileStorageService.readStoredFile(netdiskFile.getPath()); // 从磁盘读取当前的物理文件字节
        byte[] newStoredBytes = (targetEncryptedFlag == encrypted) // 根据目标加密状态选择加密或解密操作
            ? EncryptionUtil.encrypt(storedBytes, password) // 目标为加密：用密码加密字节数据
            : EncryptionUtil.decrypt(storedBytes, password); // 目标为解密：用密码解密字节数据
        fileStorageService.deleteStoredFile(netdiskFile.getPath()); // 删除旧的物理文件，避免磁盘冗余
        String newPath = fileStorageService.saveCompressedFile(newStoredBytes, userId, netdiskFile.getFileId()); // 将处理后的字节写入新的物理文件
        adjustStorageOnSizeChange(userId, netdiskFile.getFileSize(), newStoredBytes.length); // 调整存储空间使用量（加密可能改变文件大小）

        netdiskFile.setPath(newPath); // 更新文件物理路径为新的存储路径
        netdiskFile.setFileSize((long) newStoredBytes.length); // 更新文件大小为加密/解密后的实际大小
        netdiskFile.setDirId(targetDirId); // 更新文件所属目录ID（移入/移出私密空间会改变目录）
        netdiskFile.setIsEncrypted(targetEncryptedFlag); // 更新加密标志
        return fileMapper.updateFile(netdiskFile) > 0; // 将以上变更写入数据库并返回是否成功
    }

    private void updateStorageUsed(StorageSpace storageSpace, long delta) { // 更新存储空间使用量：delta为正增加占用，为负释放空间
        storageSpace.setUsedSpace(storageSpace.getUsedSpace() + delta); // 已用空间累加增量（正或负）
        storageSpace.setRemainSpace(storageSpace.getTotalSpace() - storageSpace.getUsedSpace()); // 重新计算剩余空间
        storageSpaceMapper.updateStorageSpace(storageSpace); // 将变更写入数据库
    }

    private void adjustStorageOnSizeChange(Long userId, Long oldSize, long newSize) { // 文件大小变化时调整存储空间占用
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId); // 查询用户存储空间记录
        if (storageSpace == null) return; // 无存储记录则跳过调整
        long delta = newSize - oldSize; // 计算大小变化量（正为增大，负为缩小）
        if (storageSpace.getRemainSpace() < delta) { // 剩余空间不足以容纳增量
            throw new RuntimeException("存储空间不足"); // 抛出异常中断操作
        }
        updateStorageUsed(storageSpace, delta); // 更新存储空间使用量
    }

    void releaseStorage(Long userId, Long fileSize) { // 释放指定大小的存储空间（文件删除或移入回收站时调用）
        StorageSpace storageSpace = storageSpaceMapper.selectByUserId(userId); // 查询用户存储空间记录
        if (storageSpace == null || fileSize == null) return; // 无存储记录或文件大小为空则跳过
        storageSpace.setUsedSpace(Math.max(0, storageSpace.getUsedSpace() - fileSize)); // 减去文件大小，确保不会出现负数
        storageSpace.setRemainSpace(storageSpace.getTotalSpace() - storageSpace.getUsedSpace()); // 重新计算剩余空间
        storageSpaceMapper.updateStorageSpace(storageSpace); // 将变更写入数据库
    }

    private boolean resolveConflict(Long userId, Long dirId, String fileName, boolean force) { // 检查并处理同名文件冲突：force模式删除旧文件，否则抛异常
        if (fileMapper.countFilesByName(userId, dirId, fileName) > 0) { // 查询目标目录下是否存在同名文件
            if (!force) throw new FileConflictException(fileName); // 非强制模式，抛出冲突异常由上层处理
            List<NetdiskFile> files = fileMapper.selectFilesByNameAndDir(userId, dirId, fileName, fileStatusNormal); // 强制模式，查找所有同名文件
            for (NetdiskFile f : files) { // 遍历同名文件列表
                if (fileName.equals(f.getFileName())) { // 精确匹配文件名
                    fileStorageService.deleteStoredFile(f.getPath()); // 删除旧文件的物理磁盘文件
                    releaseStorage(userId, f.getFileSize()); // 释放旧文件占用的存储空间
                    fileMapper.deleteFile(f.getFileId(), userId); // 从数据库中删除旧文件记录
                    break; // 只删除第一个精确匹配的文件
                }
            }
        }
        return true; // 无冲突或冲突已解决
    }

    public String uniqueFileName(Long userId, Long dirId, String fileName) { // 为文件名自动追加序号后缀，确保同名文件不覆盖（用于上传等场景）
        String base = fileName; // 基础文件名（不含后缀）
        String ext = ""; // 文件扩展名（含点号）
        int dot = fileName.lastIndexOf('.'); // 查找最后一个点号位置
        if (dot > 0 && dot < fileName.length() - 1) { // 点号不在首位也不在末位，视为有效的扩展名分隔符
            base = fileName.substring(0, dot); // 截取基础文件名
            ext = fileName.substring(dot); // 截取扩展名（含点号）
        }
        String candidate = fileName; // 候选文件名，从原始名开始尝试
        int n = 1; // 重名序号计数器
        while (fileMapper.countFilesByName(userId, dirId, candidate) > 0) { // 查询候选名是否存在
            candidate = base + " (" + n + ")" + ext; // 存在重名则拼接 " (n)" 序号
            n++; // 递增序号
        }
        return candidate; // 返回唯一的文件名
    }

    private Long resolveEncryptTarget(Long userId, Long dirId, boolean encrypt, String privatePassword) { // 加密上传时解析目标目录：非加密直接用传入dirId，加密文件强制放在私密空间
        if (!encrypt) { // 非加密上传
            Directory d = directoryMapper.selectDirectoryById(dirId, userId); // 校验目录存在性
            if (d == null) throw new RuntimeException("目录不存在"); // 目录不存在则拒绝
            return dirId; // 直接返回传入的目录ID
        }
        privateSpaceService.validatePrivatePassword(userId, privatePassword); // 加密上传需先验证私密空间密码
        Directory privateRoot = privateSpaceService.findPrivateSpaceRoot(userId); // 查找用户的私密空间根目录
        if (privateRoot == null) throw new RuntimeException("私密空间根目录不存在，请先启用私密空间"); // 未启用私密空间则拒绝
        Long target = isInPrivateSpace(userId, dirId, privateRoot.getDirId()) ? dirId : privateRoot.getDirId(); // 若传入目录已在私密空间内则保持，否则使用根目录
        Directory d = directoryMapper.selectDirectoryById(target, userId); // 再次校验目标目录存在性
        if (d == null) throw new RuntimeException("目录不存在"); // 目录不存在则拒绝
        return target; // 返回最终目标目录ID
    }

    private StorageSpace requireStorage(Long userId, long size) { // 检查用户存储空间是否足够容纳指定大小的文件
        StorageSpace sp = storageSpaceMapper.selectByUserId(userId); // 查询用户存储空间记录
        if (sp == null || sp.getRemainSpace() < size) throw new RuntimeException("存储空间不足"); // 无存储记录或剩余空间不足
        return sp; // 返回存储空间记录供后续使用
    }

    private NetdiskFile insertAndSaveFile(Long userId, Long targetDirId, String displayName,
            long storedSize, byte[] processed, String compressMethod, int isEncryptedFlag,
            StorageSpace storageSpace) throws IOException { // 插入文件数据库记录并写入物理文件，同时更新存储空间
        displayName = uniqueFileName(userId, targetDirId, displayName); // 确保文件名在目标目录下唯一
        NetdiskFile f = new NetdiskFile(); // 创建文件实体对象
        f.setFileName(displayName); // 设置唯一化后的文件名
        f.setFileType(FileUtils.extractExtension(displayName)); // 根据文件名自动提取扩展名
        f.setFileSize(storedSize); // 设置文件处理后的实际字节大小
        f.setUserId(userId); // 绑定文件归属用户
        f.setDirId(targetDirId); // 设置文件所属目录ID
        f.setStatus(fileStatusNormal); // 文件状态设为正常
        f.setIsEncrypted(isEncryptedFlag); // 设置加密标志
        f.setCompressMethod((int) compressCode(compressMethod)); // 设置压缩算法编码
        fileMapper.insertFile(f); // 插入文件记录到数据库，获取自增主键fileId
        f.setPath(fileStorageService.saveCompressedFile(processed, userId, f.getFileId())); // 将处理后的字节写入磁盘物理文件，设置文件路径
        fileMapper.updateFile(f); // 回写物理文件路径到数据库记录
        updateStorageUsed(storageSpace, storedSize); // 增加存储空间已使用量
        return f; // 返回完整的文件实体（含fileId和path）
    }

    private boolean isInPrivateSpace(Long userId, Long dirId, Long privateRootId) { // 检查指定目录是否在私密空间根目录的子树内
        if (dirId == null || privateRootId == null) return false; // 任一参数为空，不在私密空间内
        if (dirId.equals(privateRootId)) return true; // 目标目录就是私密空间根目录本身
        Long cur = dirId; // 从目标目录开始向上追溯父目录链
        for (int i = 0; i < 20; i++) { // 最多追溯20层，防止死循环
            Directory d = directoryMapper.selectDirectoryById(cur, userId); // 查询当前目录
            if (d == null || d.getParentDirId() == null) return false; // 查到根（parentDirId为空）还没匹配，说明不在私密空间子树内
            if (d.getParentDirId().equals(privateRootId)) return true; // 父目录是私密空间根目录，匹配成功
            cur = d.getParentDirId(); // 继续向上一级追溯
        }
        return false; // 超过最大层级仍未匹配，视为不在私密空间内
    }

}
