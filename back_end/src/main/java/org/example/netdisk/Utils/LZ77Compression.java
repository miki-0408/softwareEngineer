package org.example.netdisk.Utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 手动实现的 LZ77 滑动窗口压缩算法
 *
 * ██████  设计原理  ██████
 *
 * LZ77 是 DEFLATE（GZip/PNG）的基础算法。
 * 核心观察：数据中经常有重复出现的字符串，与其重复存储，不如存一个
 * 引用："从 N 个字节前的位置复制 L 个字节"。
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │  滑动窗口                       前瞻缓冲区                │
 * │  ┌──────────────────┬──────────────────────────┐         │
 * │  │ 已编码的区域       │ 待编码的区域              │         │
 * │  │ (搜索字典)        │ (look-ahead buffer)      │         │
 * │  └──────────────────┴──────────────────────────┘         │
 * │  ←──── 窗口大小 ────→│←── 前瞻大小 ──→│                  │
 * │                       ↑                                  │
 * │                    当前处理位置                            │
 * │                                                          │
 * │  在窗口中找最长匹配 → 输出 (distance, length)              │
 * │  找不到匹配 → 输出 literal byte                           │
 * └──────────────────────────────────────────────────────────┘
 *
 * ██████  本实现参数  ██████
 *   - 窗口大小: 4096 字节
 *   - 前瞻缓冲区: 16 字节
 *   - 最小匹配长度: 3 字节（更短的不如直接存 literal）
 *
 * ██████  输出格式  ██████
 *   每个 token 由一个 flag byte 开头：
 *     flag=0x00 → literal 模式：后跟 1 个原始字节
 *     flag=0x01 → match 模式：后跟 2 字节距离 + 1 字节长度
 *     flag=0xFF → 结束标记
 */
public class LZ77Compression {

    // ======================== 参数常量 ========================

    /** 滑动窗口大小（搜索字典的范围） */
    private static final int WINDOW_SIZE = 4096;

    /** 前瞻缓冲区大小（一次最多向前看多少字节） */
    private static final int LOOKAHEAD_SIZE = 16;

    /** 最小匹配长度（小于这个值用 literal 更划算） */
    private static final int MIN_MATCH = 3;

    // ======================== Token 类型标记 ========================

    private static final byte TOKEN_LITERAL = 0x00;
    private static final byte TOKEN_MATCH = 0x01;
    private static final byte TOKEN_END = (byte) 0xFF;

    // ======================== 公开接口 ========================

    /**
     * 压缩数据
     * @param data 原始字节
     * @return 压缩后的字节
     */
    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = 0;

        while (pos < data.length) {
            // 在窗口中查找最长匹配
            Match match = findLongestMatch(data, pos);

            if (match != null && match.length >= MIN_MATCH) {
                // 找到匹配 → 输出 match token
                out.write(TOKEN_MATCH);
                // 距离用 2 字节小端序存储
                out.write(match.distance & 0xFF);
                out.write((match.distance >> 8) & 0xFF);
                // 长度存储
                out.write(match.length - MIN_MATCH); // 减 MIN_MATCH 以节省空间
                pos += match.length;
            } else {
                // 没找到匹配 → 输出 literal token
                out.write(TOKEN_LITERAL);
                out.write(data[pos] & 0xFF);
                pos++;
            }
        }

        // 结束标记
        out.write(TOKEN_END);

        return out.toByteArray();
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

        // 用可增长的缓冲区替代 ByteArrayOutputStream，避免每次 match 都 toByteArray()
        byte[] buf = new byte[Math.max(data.length * 2, 8192)];
        int outPos = 0;
        int pos = 0;

        while (pos < data.length) {
            int flag = data[pos] & 0xFF;
            pos++;

            if (flag == (TOKEN_END & 0xFF)) {
                break;
            } else if (flag == (TOKEN_LITERAL & 0xFF)) {
                if (outPos >= buf.length) buf = grow(buf, outPos);
                buf[outPos++] = data[pos++];
            } else if (flag == (TOKEN_MATCH & 0xFF)) {
                int distance = (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
                int length = (data[pos + 2] & 0xFF) + MIN_MATCH;
                pos += 3;

                int srcPos = outPos - distance;
                while (outPos + length > buf.length) buf = grow(buf, outPos);
                for (int i = 0; i < length; i++) {
                    buf[outPos++] = buf[srcPos + i];
                }
            } else {
                throw new RuntimeException("解压失败：未知的 token 类型 " + flag);
            }
        }

        return Arrays.copyOf(buf, outPos);
    }

    // ======================== 匹配查找 ========================

    /**
     * 在滑动窗口中查找与当前位置数据的最长匹配
     *
     * @param data 完整数据
     * @param pos  当前处理位置
     * @return 匹配信息 (distance, length)，无匹配则返回 null
     */
    private static Match findLongestMatch(byte[] data, int pos) {
        // 窗口的开始位置（不能小于 0）
        int windowStart = Math.max(0, pos - WINDOW_SIZE);
        // 前瞻缓冲区的结束位置（不能超出数据末尾）
        int lookaheadEnd = Math.min(data.length, pos + LOOKAHEAD_SIZE);
        // 最大可能匹配长度
        int maxPossibleLen = lookaheadEnd - pos;

        if (maxPossibleLen < MIN_MATCH) {
            return null;
        }

        int bestDistance = 0;
        int bestLength = 0;

        // 在窗口中逐字节搜索
        for (int winPos = windowStart; winPos < pos; winPos++) {
            // 计算当前位置能匹配多长
            int matchLen = 0;
            while (matchLen < maxPossibleLen
                    && (pos + matchLen) < data.length
                    && (winPos + matchLen) < pos
                    && data[winPos + matchLen] == data[pos + matchLen]) {
                matchLen++;
            }

            if (matchLen > bestLength) {
                bestLength = matchLen;
                bestDistance = pos - winPos;
                // 如果已经达到了最大可能长度，提前退出
                if (bestLength == maxPossibleLen) {
                    break;
                }
            }
        }

        if (bestLength >= MIN_MATCH) {
            return new Match(bestDistance, bestLength);
        }
        return null;
    }

    private static byte[] grow(byte[] old, int minSize) {
        byte[] next = new byte[Math.max(old.length * 2, minSize + 4096)];
        System.arraycopy(old, 0, next, 0, Math.min(minSize, old.length));
        return next;
    }

    // ======================== 匹配结果 ========================

    /** 匹配信息 */
    private static class Match {
        final int distance;  // 回退距离
        final int length;    // 匹配长度

        Match(int distance, int length) {
            this.distance = distance;
            this.length = length;
        }
    }

    // ======================== 演示入口 ========================

    public static void main(String[] args) {
        // 构建一个有明显重复的测试数据
        StringBuilder sb = new StringBuilder();
        sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        sb.append("abcdefghijklmnopqrstuvwxyz");
        sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");  // 重复
        sb.append("1234567890");
        sb.append("abcdefghijklmnopqrstuvwxyz");  // 重复
        sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");  // 重复
        sb.append("!!! This is the end !!!");

        byte[] original = sb.toString().getBytes(StandardCharsets.UTF_8);
        System.out.println("原始大小: " + original.length + " 字节");

        byte[] compressed = compress(original);
        System.out.println("压缩后: " + compressed.length + " 字节");
        System.out.println("压缩率: " + String.format("%.1f%%",
                (double) compressed.length / original.length * 100));

        byte[] decompressed = decompress(compressed);
        System.out.println("解压后: " + decompressed.length + " 字节");

        boolean match = Arrays.equals(original, decompressed);
        System.out.println("一致性: " + match);

        if (!match) {
            System.out.println("原始: " + new String(original, StandardCharsets.UTF_8));
            System.out.println("解压: " + new String(decompressed, StandardCharsets.UTF_8));
        }

        // 测试英文文本的压缩率
        System.out.println("\n--- 英文文本测试 ---");
        byte[] textData = ("The LZ77 compression algorithm is a sliding window compression " +
                "algorithm that was published in 1977 by Abraham Lempel and Jacob Ziv. " +
                "It forms the basis of many modern compression formats including DEFLATE, " +
                "which is used in PNG images and GZip compression. The algorithm works by " +
                "finding repeated sequences of bytes in the data and replacing them with " +
                "references to previous occurrences.").getBytes(StandardCharsets.UTF_8);
        System.out.println("原始: " + textData.length + " 字节");
        byte[] textCompressed = compress(textData);
        System.out.println("压缩: " + textCompressed.length + " 字节");
        System.out.println("压缩率: " + String.format("%.1f%%",
                (double) textCompressed.length / textData.length * 100));
        byte[] textDecompressed = decompress(textCompressed);
        System.out.println("一致性: " + Arrays.equals(textData, textDecompressed));
    }
}
