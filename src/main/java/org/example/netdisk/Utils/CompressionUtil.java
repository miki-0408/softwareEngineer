package org.example.netdisk.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionUtil {

    public static byte[] compress(byte[] data) {
        if (data == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("文件压缩失败", e);
        }
    }

    public static byte[] decompress(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("文件解压失败", e);
        }
    }
}
