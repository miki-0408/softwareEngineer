package org.example.netdisk.Utils;

public class FileUtils {

    public static String extractExtension(String fileName) { // 从文件名提取小写扩展名（不含点），用于判断文件类型
        if (fileName == null) return null; // 空文件名返回null
        int idx = fileName.lastIndexOf('.'); // 查找最后一个点的位置
        if (idx < 0 || idx == fileName.length() - 1) return null; // 无扩展名或以点结尾时返回null
        return fileName.substring(idx + 1).toLowerCase(); // 截取点之后的扩展名并转为小写
    }
}
