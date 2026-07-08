package org.example.netdisk.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * POSIX ustar 兼容的 TAR 归档工具
 *
 * 头部结构（512 字节）:
 *   name[100] mode[8] uid[8] gid[8] size[12] mtime[12]
 *   chksum[8] typeflag[1] linkname[100] magic[6] version[2]
 *   uname[32] gname[32] devmajor[8] devminor[8] prefix[155]
 */
public class TarUtil {

    private static final int BLOCK_SIZE = 512;
    private static final byte TYPE_FILE = '0';

    public static byte[] createTar(List<TarEntry> entries) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        for (TarEntry entry : entries) {
            byte[] header = buildHeader(entry);
            bos.write(header);

            if (entry.data != null && entry.data.length > 0) {
                bos.write(entry.data);
                int padding = (BLOCK_SIZE - (entry.data.length % BLOCK_SIZE)) % BLOCK_SIZE;
                if (padding > 0) bos.write(new byte[padding]);
            }
        }
        bos.write(new byte[BLOCK_SIZE * 2]);
        return bos.toByteArray();
    }

    public static List<TarEntry> extractTar(byte[] tarData) throws IOException {
        List<TarEntry> entries = new ArrayList<>();
        ByteArrayInputStream bis = new ByteArrayInputStream(tarData);
        byte[] headerBuf = new byte[BLOCK_SIZE];

        while (true) {
            int read = bis.read(headerBuf);
            if (read < BLOCK_SIZE) break;
            if (isZeroBlock(headerBuf)) break;

            String name = readCString(headerBuf, 0, 100);
            if (name.isEmpty()) break;

            long size = readOctal(headerBuf, 124, 12);
            byte type = headerBuf[156];

            byte[] data = new byte[0];
            if (type == TYPE_FILE && size > 0) {
                data = new byte[(int) size];
                bis.read(data);
                int skip = (int) ((BLOCK_SIZE - (size % BLOCK_SIZE)) % BLOCK_SIZE);
                if (skip > 0) bis.skip(skip);
            }
            entries.add(new TarEntry(name, data, false));
        }
        return entries;
    }

    /** 构建标准 ustar 512 字节头部 */
    private static byte[] buildHeader(TarEntry entry) {
        byte[] header = new byte[BLOCK_SIZE];

        writeCString(header, 0, 100, entry.name);
        writeOctal(header, 100, 8, 0100644);              // mode = regular file
        writeOctal(header, 108, 8, 0);                    // uid
        writeOctal(header, 116, 8, 0);                    // gid
        writeOctal(header, 124, 12, entry.data != null ? entry.data.length : 0);
        writeOctal(header, 136, 12, System.currentTimeMillis() / 1000);
        header[156] = TYPE_FILE;
        writeCString(header, 257, 6, "ustar");
        writeCString(header, 263, 2, "00");
        writeCString(header, 265, 32, "netdisk");
        writeCString(header, 297, 32, "netdisk");

        // 校验和：先填充空格再算
        Arrays.fill(header, 148, 156, (byte) ' ');
        int checksum = 0;
        for (byte b : header) checksum += (b & 0xFF);

        // 校验和格式: 6 位八进制 + \0 + ' '
        String cs = Integer.toOctalString(checksum);
        if (cs.length() > 6) cs = cs.substring(cs.length() - 6);
        int pad = 6 - cs.length();
        for (int i = 0; i < pad; i++) header[148 + i] = '0';
        for (int i = 0; i < cs.length(); i++) header[148 + pad + i] = (byte) cs.charAt(i);
        header[154] = 0;
        header[155] = (byte) ' ';

        return header;
    }

    /** 写入 null 终止字符串（不溢出不补零） */
    private static void writeCString(byte[] buf, int offset, int len, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int n = Math.min(bytes.length, len - 1);
        System.arraycopy(bytes, 0, buf, offset, n);
        buf[offset + n] = 0;
    }

    /** 读取 null 终止字符串 */
    private static String readCString(byte[] buf, int offset, int len) {
        int end = offset;
        while (end < offset + len && buf[end] != 0) end++;
        return new String(buf, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    /** 写入 POSIX 八进制字段：左补 '0' + 数字 + \0 终止 */
    private static void writeOctal(byte[] buf, int offset, int len, long value) {
        String s = Long.toOctalString(value);
        if (s.length() > len - 2) s = s.substring(s.length() - (len - 2));
        int pad = len - 1 - s.length();
        for (int i = 0; i < pad; i++) buf[offset + i] = (byte) '0';
        for (int i = 0; i < s.length(); i++) buf[offset + pad + i] = (byte) s.charAt(i);
        buf[offset + len - 1] = 0;
    }

    /** 读取八进制字段（遇 \0 或空格停止） */
    private static long readOctal(byte[] buf, int offset, int len) {
        long result = 0;
        for (int i = offset; i < offset + len; i++) {
            byte b = buf[i];
            if (b == 0 || b == ' ') break;
            if (b >= '0' && b <= '7') result = result * 8 + (b - '0');
        }
        return result;
    }

    private static boolean isZeroBlock(byte[] buf) {
        for (byte b : buf) if (b != 0) return false;
        return true;
    }

    public static class TarEntry {
        public final String name;
        public final byte[] data;
        public final boolean isDir;

        public TarEntry(String name, byte[] data, boolean isDir) {
            this.name = name;
            this.data = data;
            this.isDir = isDir;
        }
    }
}
