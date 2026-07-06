package org.example.netdisk.Service.Support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.example.PCOI.Service.Support.Enum.illustration;

@Service
public class FileStorageService {

    @Value("${pcoi.upload.base-dir}")
    private String uploadBaseDir;
    @Value("${pcoi.upload.url-prefix}")
    private String uploadUrlPrefix;
    // 每个用户目录下的三个固定子目录名
    @Value("${pcoi.upload.avatar-subdir:avatar}")
    private String avatarSubdir;
    @Value("${pcoi.upload.illustration-subdir:illustration}")
    private String illustrationSubdir;
    @Value("${pcoi.upload.manga-subdir:manga}")
    private String mangaSubdir;

    // 头像：保存到 baseDir/{userId}/avatar/ 下，更新时清空旧头像；返回“实际文件 URL”
    public String saveAvatar(MultipartFile avatar, String userId) {
        if (avatar == null || avatar.isEmpty()) return null;
        String relFolder = Paths.get(sanitize(userId), avatarSubdir).toString();
        Path dir = Paths.get(uploadBaseDir, relFolder);
        try {
            Files.createDirectories(dir);
            String filename = saveOneFile(avatar, dir);
            return buildFileUrl(relFolder, filename);
        } catch (IOException e) {
            throw new RuntimeException("仅支持图片类型文件", e);
        }
    }

    // 作品：统一按 List 存至 baseDir/{userId}/{illustration|manga}/{workId}/，返回每个文件的 URL 列表
    public List<String> saveWorkImages(List<MultipartFile> files, Integer type, String userId) {
        if (files == null || files.isEmpty() || type == null) return null;
        String typeDir = type.equals(illustration) ? illustrationSubdir : mangaSubdir;
        String workId = UUID.randomUUID().toString().replace("-", "");
        String relFolder = Paths.get(sanitize(userId), typeDir, workId).toString();
        Path dir = Paths.get(uploadBaseDir, relFolder);

        List<MultipartFile> list = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) list.add(f);
        }
        if (list.isEmpty()) return null;

        try {
            Files.createDirectories(dir);
            List<String> urls = new ArrayList<>(list.size());
            for (MultipartFile f : list) {
                String filename = saveOneFile(f, dir);
                urls.add(buildFileUrl(relFolder, filename));
            }
            return urls;
        } catch (IOException e) {
            throw new RuntimeException("保存作品失败", e);
        }
    }

    // 保存单文件到指定目录（校验为图片类型；文件名使用 UUID+扩展名），返回最终文件名
    private String saveOneFile(MultipartFile file, Path targetDir) throws IOException {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片类型文件");
        }
        String ext = getFileExtension(Objects.requireNonNullElse(file.getOriginalFilename(), ""));
        if (ext == null && contentType != null) {
            ext = mimeToExt(contentType);
        }
        String filename = UUID.randomUUID().toString().replace("-", "");
        filename = (ext == null || ext.isEmpty()) ? filename : filename + "." + ext;
        Path dest = targetDir.resolve(filename);
        file.transferTo(dest.toFile());
        return filename;
    }

    // 生成“文件夹 URL”（以 / 结尾）
    private String buildFolderUrl(String relFolder) {
        String url = ensureTrailingSlash(uploadUrlPrefix) + (relFolder.startsWith("/") ? relFolder.substring(1) : relFolder);
        return ensureTrailingSlash(url);
    }

    // 生成“文件 URL”（不以 / 结尾）
    private String buildFileUrl(String relFolder, String filename) {
        String folderUrl = buildFolderUrl(relFolder); // 以 / 结尾
        return folderUrl + filename;
    }

    private static String ensureTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "/";
        return s.endsWith("/") ? s : s + "/";
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "unknown";
        return s.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String getFileExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return null;
        return filename.substring(idx + 1).toLowerCase();
    }

    private static String mimeToExt(String mime) {
        if (mime == null) return null;
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