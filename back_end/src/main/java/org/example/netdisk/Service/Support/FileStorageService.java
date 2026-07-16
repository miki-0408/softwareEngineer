package org.example.netdisk.Service.Support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.example.netdisk.Utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j // Lombok日志注解
@Service
public class FileStorageService {

    @Value("${netdisk.upload.base-dir}") // 从配置文件注入文件存储根目录
    private String uploadBaseDir;
    @Value("${netdisk.upload.url-prefix}") // 从配置文件注入URL访问前缀
    private String uploadUrlPrefix;
    @Value("${netdisk.upload.avatar-subdir:avatar}") // 头像存储子目录，默认avatar
    private String avatarSubdir;
    @Value("${netdisk.upload.file-subdir:files}") // 普通文件存储子目录，默认files
    private String fileSubdir;

    public String saveAvatar(MultipartFile avatar, Long userId) { // 保存用户头像，返回访问URL
        if (avatar == null || avatar.isEmpty()) { // 空文件不保存
            return null;
        }
        if (avatar.getSize() > 10 * 1024 * 1024) { // 限制头像大小不超过10MB
            throw new RuntimeException("头像文件大小不能超过 10MB");
        }
        String contentType = avatar.getContentType(); // 获取MIME类型
        if (contentType != null && !contentType.startsWith("image/")) { // 仅允许图片类型
            throw new RuntimeException("仅支持图片类型文件");
        }
        String relFolder = Paths.get(sanitize(userId), avatarSubdir).toString(); // 构建相对路径：{userId}/avatar
        Path dir = Paths.get(uploadBaseDir, relFolder); // 拼接完整存储路径
        try {
            Files.createDirectories(dir); // 确保目录存在（递归创建）
            String filename = saveOneImage(avatar, dir); // 保存图片文件并返回UUID文件名
            return buildFileUrl(relFolder, filename); // 构建外部访问URL
        } catch (IOException e) { // 文件操作异常
            log.error("头像保存失败: dir={}", dir.toAbsolutePath(), e);
            throw new RuntimeException("头像保存失败: " + e.getMessage(), e);
        }
    }

    public String saveCompressedFile(byte[] data, Long userId, Long fileId) { // 保存压缩后的文件字节数据到磁盘
        String relFolder = Paths.get(sanitize(userId), fileSubdir).toString(); // 相对路径：{userId}/files
        Path dir = Paths.get(uploadBaseDir, relFolder); // 完整路径
        try {
            Files.createDirectories(dir); // 确保目录存在
            String filename = fileId + ".dat"; // 文件名使用fileId确保唯一
            Path dest = dir.resolve(filename); // 目标文件完整路径
            Files.write(dest, data); // 直接写入字节数据
            return dest.toString(); // 返回存储的绝对路径
        } catch (IOException e) { // 文件写入异常
            throw new RuntimeException("文件保存失败", e);
        }
    }

    public byte[] readStoredFile(String path) { // 从磁盘读取存储的文件字节数据
        try {
            return Files.readAllBytes(Paths.get(path)); // 一次性读取全部字节
        } catch (IOException e) { // 文件读取异常
            throw new RuntimeException("文件读取失败", e);
        }
    }

    public void deleteStoredFile(String path) { // 删除磁盘上的存储文件
        if (path == null || path.isBlank()) { // 空路径不处理
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(path)); // 文件存在则删除，不存在则忽略
        } catch (IOException e) { // 删除异常
            throw new RuntimeException("文件删除失败", e);
        }
    }

    private String saveOneImage(MultipartFile file, Path targetDir) throws IOException { // 保存单个图片并生成UUID文件名，返回文件名
        String ext = FileUtils.extractExtension(Objects.requireNonNullElse(file.getOriginalFilename(), "")); // 从原始文件名提取扩展名
        String contentType = file.getContentType(); // 获取MIME类型
        if ((ext == null || ext.isEmpty()) && contentType != null) { // 无法从文件名获取扩展名时，从MIME类型推导
            ext = mimeToExt(contentType); // MIME类型转扩展名
        }
        String filename = UUID.randomUUID().toString().replace("-", ""); // 生成UUID作为文件名（去掉连字符）
        filename = (ext == null || ext.isEmpty()) ? filename : filename + "." + ext; // 拼接扩展名
        Path dest = targetDir.resolve(filename); // 目标文件路径
        Files.copy(file.getInputStream(), dest); // 将上传文件流写入目标文件
        return filename; // 返回生成的文件名
    }

    private String buildFileUrl(String relFolder, String filename) { // 构建文件的外部访问URL
        relFolder = relFolder.replace('\\', '/'); // 统一使用正斜杠
        String folderUrl = ensureTrailingSlash(uploadUrlPrefix) + (relFolder.startsWith("/") ? relFolder.substring(1) : relFolder); // 拼接基础URL和相对路径
        if (!folderUrl.endsWith("/")) { // 确保文件夹URL以斜杠结尾
            folderUrl = folderUrl + "/";
        }
        return folderUrl + filename; // 拼接文件名
    }

    private static String ensureTrailingSlash(String s) { // 确保字符串以斜杠结尾
        if (s == null || s.isEmpty()) { // 空字符串返回根路径
            return "/";
        }
        return s.endsWith("/") ? s : s + "/"; // 无斜杠则追加
    }

    private static String sanitize(Long userId) { // 将用户ID转为安全的路径段
        if (userId == null) { // null用户ID使用unknown
            return "unknown";
        }
        return String.valueOf(userId); // 转为字符串
    }

    private static String mimeToExt(String mime) { // MIME类型映射为文件扩展名
        if (mime == null) { // 空MIME返回null
            return null;
        }
        return switch (mime) { // JDK 17+ switch表达式匹配MIME类型
            case "image/jpeg" -> "jpg"; // JPEG图片
            case "image/png" -> "png"; // PNG图片
            case "image/gif" -> "gif"; // GIF图片
            case "image/webp" -> "webp"; // WebP图片
            case "image/bmp" -> "bmp"; // BMP图片
            default -> null; // 不支持的MIME类型返回null
        };
    }
}
