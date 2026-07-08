package org.example.netdisk.Service.Support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStorageService {

    @Value("${netdisk.upload.base-dir}")
    private String uploadBaseDir;
    @Value("${netdisk.upload.url-prefix}")
    private String uploadUrlPrefix;
    @Value("${netdisk.upload.avatar-subdir:avatar}")
    private String avatarSubdir;
    @Value("${netdisk.upload.file-subdir:files}")
    private String fileSubdir;

    public String saveAvatar(MultipartFile avatar, Long userId) {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }
        String contentType = avatar.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new RuntimeException("仅支持图片类型文件");
        }
        String relFolder = Paths.get(sanitize(userId), avatarSubdir).toString();
        Path dir = Paths.get(uploadBaseDir, relFolder);
        try {
            Files.createDirectories(dir);
            String filename = saveOneImage(avatar, dir);
            return buildFileUrl(relFolder, filename);
        } catch (IOException e) {
            log.error("头像保存失败: dir={}", dir.toAbsolutePath(), e);
            throw new RuntimeException("头像保存失败: " + e.getMessage(), e);
        }
    }

    public String saveCompressedFile(byte[] data, Long userId, Long fileId) {
        String relFolder = Paths.get(sanitize(userId), fileSubdir).toString();
        Path dir = Paths.get(uploadBaseDir, relFolder);
        try {
            Files.createDirectories(dir);
            String filename = fileId + ".dat";
            Path dest = dir.resolve(filename);
            Files.write(dest, data);
            return dest.toString();
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }
    }

    public byte[] readStoredFile(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }

    public void deleteStoredFile(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("文件删除失败", e);
        }
    }

    private String saveOneImage(MultipartFile file, Path targetDir) throws IOException {
        String ext = getFileExtension(Objects.requireNonNullElse(file.getOriginalFilename(), ""));
        String contentType = file.getContentType();
        if ((ext == null || ext.isEmpty()) && contentType != null) {
            ext = mimeToExt(contentType);
        }
        String filename = UUID.randomUUID().toString().replace("-", "");
        filename = (ext == null || ext.isEmpty()) ? filename : filename + "." + ext;
        Path dest = targetDir.resolve(filename);
        Files.copy(file.getInputStream(), dest);
        return filename;
    }

    private String buildFileUrl(String relFolder, String filename) {
        relFolder = relFolder.replace('\\', '/');
        String folderUrl = ensureTrailingSlash(uploadUrlPrefix) + (relFolder.startsWith("/") ? relFolder.substring(1) : relFolder);
        if (!folderUrl.endsWith("/")) {
            folderUrl = folderUrl + "/";
        }
        return folderUrl + filename;
    }

    private static String ensureTrailingSlash(String s) {
        if (s == null || s.isEmpty()) {
            return "/";
        }
        return s.endsWith("/") ? s : s + "/";
    }

    private static String sanitize(Long userId) {
        if (userId == null) {
            return "unknown";
        }
        return String.valueOf(userId);
    }

    private static String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return null;
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    private static String mimeToExt(String mime) {
        if (mime == null) {
            return null;
        }
        return switch (mime) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            default -> null;
        };
    }
}
