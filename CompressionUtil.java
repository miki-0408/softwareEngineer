package org.example.netdisk.Utils;

/**
 * 压缩 / 解压缩工具
 *
 * ██████  当前实现已清空  ██████
 * 请在下方 TODO 处填入你选择的压缩算法。
 *
 * 建议手动实现的算法：
 * ┌──────────────────────────────────────────────────────────────┐
 * │ ① LZ77 滑动窗口压缩 (LZ77Compression.java)                   │
 * │    - 压缩鼻祖算法，GZip/Deflate/PNG 的基础                    │
 * │    - 核心思想：用 (距离, 长度) 对代替重复出现的字符串          │
 * │    - 参考完整实现见 LZ77Compression.java                      │
 * ├──────────────────────────────────────────────────────────────┤
 * │ ② RLE 游程编码                                              │
 * │    - 最简单，将连续重复的字节替换为 (计数值, 字节值)           │
 * │    - 适合有大量连续重复字节的数据（如 BMP 图像）              │
 * │    - 对普通文件压缩率极差，适合作为入门练习                    │
 * └──────────────────────────────────────────────────────────────┘
 *
 * 标准参考实现见 StandardCompression.java（GZIP）
 */
public class CompressionUtil {

    /**
     * 压缩数据
     * @param data 原始字节
     * @return 压缩后的字节
     */
    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        return LZ77Compression.compress(data);
    }

    /**
     * 解压缩数据
     * @param data 压缩后的字节
     * @return 解压后的原始字节
     */
    public static byte[] decompress(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        return LZ77Compression.decompress(data);
    }
}
