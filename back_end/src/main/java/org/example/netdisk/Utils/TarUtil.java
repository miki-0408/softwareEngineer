package org.example.netdisk.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TarUtil {

    private static final int BLOCK_SIZE = 512; // TAR标准块大小：每个头部和数据块对齐到512字节
    private static final byte TYPE_FILE = '0'; // 文件类型标识：'0'表示普通文件

    public static byte[] createTar(List<TarEntry> entries) throws IOException { // 将文件条目列表打包为TAR归档字节数组
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        for (TarEntry entry : entries) { // 遍历所有条目
            byte[] header = buildHeader(entry); // 为每个条目构建512字节头部
            bos.write(header); // 写入头部

            if (entry.data != null && entry.data.length > 0) { // 有数据的文件条目
                bos.write(entry.data); // 写入文件数据
                int padding = (BLOCK_SIZE - (entry.data.length % BLOCK_SIZE)) % BLOCK_SIZE; // 计算填充字节数，对齐到512边界
                if (padding > 0) bos.write(new byte[padding]); // 写入零填充
            }
        }
        bos.write(new byte[BLOCK_SIZE * 2]); // 写入1024字节结束标记（两个全零块）
        return bos.toByteArray(); // 返回完整TAR归档
    }

    public static List<TarEntry> extractTar(byte[] tarData) throws IOException { // 从TAR归档字节数组中提取文件条目列表
        List<TarEntry> entries = new ArrayList<>(); // 结果列表
        ByteArrayInputStream bis = new ByteArrayInputStream(tarData); // 输入流
        byte[] headerBuf = new byte[BLOCK_SIZE]; // 头部缓冲区

        while (true) { // 循环读取每个条目
            int read = bis.read(headerBuf); // 读取512字节头部
            if (read < BLOCK_SIZE) break; // 读取不足一个块则结束
            if (isZeroBlock(headerBuf)) break; // 遇到全零块则结束

            String name = readCString(headerBuf, 0, 100); // 读取文件名（最多100字节）
            if (name.isEmpty()) break; // 空文件名表示结束

            long size = readOctal(headerBuf, 124, 12); // 读取文件大小（八进制字段）
            byte type = headerBuf[156]; // 读取类型标识

            byte[] data = new byte[0]; // 文件数据，默认空
            if (type == TYPE_FILE && size > 0) { // 普通文件且有数据时读取
                data = new byte[(int) size]; // 分配数据缓冲区
                bis.read(data); // 读取文件数据
                int skip = (int) ((BLOCK_SIZE - (size % BLOCK_SIZE)) % BLOCK_SIZE); // 计算对齐填充量
                while (skip > 0) skip -= bis.skip(skip); // 跳过填充字节
            }
            entries.add(new TarEntry(name, data, false)); // 添加到结果列表
        }
        return entries; // 返回提取的条目
    }

    private static byte[] buildHeader(TarEntry entry) { // 构建标准ustar格式512字节头部
        byte[] header = new byte[BLOCK_SIZE]; // 512字节头部数组

        writeCString(header, 0, 100, entry.name); // 写入文件名（0-99字节）
        writeOctal(header, 100, 8, 33188); // 写入文件权限模式（0100644八进制 = 33188十进制）
        writeOctal(header, 108, 8, 0); // 写入uid（所有者ID）
        writeOctal(header, 116, 8, 0); // 写入gid（组ID）
        writeOctal(header, 124, 12, entry.data != null ? entry.data.length : 0); // 写入文件大小
        writeOctal(header, 136, 12, System.currentTimeMillis() / 1000); // 写入修改时间（Unix时间戳）
        header[156] = TYPE_FILE; // 写入类型标识（普通文件）
        writeCString(header, 257, 6, "ustar"); // 写入magic字段（ustar格式标识）
        writeCString(header, 263, 2, "00"); // 写入version字段
        writeCString(header, 265, 32, "netdisk"); // 写入uname（用户名）
        writeCString(header, 297, 32, "netdisk"); // 写入gname（组名）

        Arrays.fill(header, 148, 156, (byte) ' '); // 校验和字段先用空格填充
        int checksum = 0; // 计算校验和
        for (byte b : header) checksum += (b & 0xFF); // 累加所有字节的无符号值

        String cs = Integer.toOctalString(checksum); // 转为八进制字符串
        if (cs.length() > 6) cs = cs.substring(cs.length() - 6); // 超过6位则截取低6位
        int pad = 6 - cs.length(); // 计算前导零个数
        for (int i = 0; i < pad; i++) header[148 + i] = '0'; // 写入前导零
        for (int i = 0; i < cs.length(); i++) header[148 + pad + i] = (byte) cs.charAt(i); // 写入八进制校验和
        header[154] = 0; // 校验和以null结尾
        header[155] = (byte) ' '; // 后跟空格

        return header; // 返回构建好的头部
    }

    private static void writeCString(byte[] buf, int offset, int len, String s) { // 写入null终止的C字符串（不溢出、不补零）
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8); // 字符串转UTF-8字节
        int n = Math.min(bytes.length, len - 1); // 最多写len-1字节（留1字节给终止null）
        System.arraycopy(bytes, 0, buf, offset, n); // 复制字符串字节
        buf[offset + n] = 0; // 写入null终止符
    }

    private static String readCString(byte[] buf, int offset, int len) { // 读取null终止的C字符串
        int end = offset; // 查找null结束位置
        while (end < offset + len && buf[end] != 0) end++; // 遇到0或到达边界停止
        return new String(buf, offset, end - offset, StandardCharsets.UTF_8).trim(); // 截取字符串并去除空白
    }

    private static void writeOctal(byte[] buf, int offset, int len, long value) { // 写入POSIX八进制字段：左补零+数字+null终止
        String s = Long.toOctalString(value); // 数值转八进制字符串
        if (s.length() > len - 2) s = s.substring(s.length() - (len - 2)); // 超出则截取低位（保留null终止符空间）
        int pad = len - 1 - s.length(); // 计算前导'0'的个数
        for (int i = 0; i < pad; i++) buf[offset + i] = (byte) '0'; // 写入前导零
        for (int i = 0; i < s.length(); i++) buf[offset + pad + i] = (byte) s.charAt(i); // 写入八进制数字
        buf[offset + len - 1] = 0; // 末尾写入null终止符
    }

    private static long readOctal(byte[] buf, int offset, int len) { // 读取八进制数值字段（遇null或空格停止）
        long result = 0; // 累加结果
        for (int i = offset; i < offset + len; i++) { // 遍历字段
            byte b = buf[i]; // 当前字节
            if (b == 0 || b == ' ') break; // 遇到null或空格则停止
            if (b >= '0' && b <= '7') result = result * 8 + b - '0'; // 累加八进制位值
        }
        return result; // 返回解析的数值
    }

    private static boolean isZeroBlock(byte[] buf) { // 判断512字节块是否全为零（TAR结束标记）
        for (byte b : buf) if (b != 0) return false; // 发现非零字节则非零块
        return true; // 全零
    }

    public static class TarEntry { // TAR条目：封装文件名、数据和类型
        public final String name; // 文件名
        public final byte[] data; // 文件数据
        public final boolean isDir; // 是否为目录

        public TarEntry(String name, byte[] data, boolean isDir) { // 构造TAR条目
            this.name = name;
            this.data = data;
            this.isDir = isDir;
        }
    }
}
