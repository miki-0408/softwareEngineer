package org.example.netdisk.Utils;

/** 通用文件工具方法 */
public class FileUtils {

    /** 从文件名提取扩展名（不含点，小写）；无法提取返回 null */
    public static String extractExtension(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1).toLowerCase();
    }
}
