package org.example.netdisk.Utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LZ77Compression {

    private static final int WINDOW_SIZE = 4096; // 滑动窗口大小，在此范围内搜索重复字符串
    private static final int LOOKAHEAD_SIZE = 16; // 前瞻缓冲区大小，一次最多向前看16字节
    private static final int MIN_MATCH = 3; // 最小匹配长度，小于3字节直接用原始字节更省空间

    private static final byte TOKEN_LITERAL = 0x00; // 原始字节标记：后跟1字节原始数据
    private static final byte TOKEN_MATCH = 0x01; // 匹配标记：后跟2字节距离+1字节长度
    private static final byte TOKEN_END = (byte) 0xFF; // 结束标记：数据流结束

    public static byte[] compress(byte[] data) { // LZ77压缩：在滑动窗口中找重复数据，用(距离,长度)替换
        if (data == null || data.length == 0) { // 空数据不处理
            return new byte[0];
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(); // 输出流
        int pos = 0; // 当前处理位置

        while (pos < data.length) { // 遍历所有数据
            Match match = findLongestMatch(data, pos); // 在窗口中查找与当前位置匹配的最长子串

            if (match != null && match.length >= MIN_MATCH) { // 找到有效匹配（长度>=3）
                out.write(TOKEN_MATCH); // 写入匹配标记
                out.write(match.distance & 0xFF); // 写入距离低8位（小端序）
                out.write((match.distance >> 8) & 0xFF); // 写入距离高8位
                out.write(match.length - MIN_MATCH); // 写入长度（减3节省空间，解压时加回）
                pos += match.length; // 跳过已匹配的字节
            } else { // 未找到匹配，输出原始字节
                out.write(TOKEN_LITERAL); // 写入原始字节标记
                out.write(data[pos] & 0xFF); // 写入原始字节
                pos++; // 前移一位
            }
        }

        out.write(TOKEN_END); // 写入结束标记

        return out.toByteArray(); // 返回压缩数据
    }

    public static byte[] decompress(byte[] data) { // LZ77解压：根据token逐条还原数据
        if (data == null || data.length == 0) { // 空数据不处理
            return new byte[0];
        }

        byte[] buf = new byte[Math.max(data.length * 2, 8192)]; // 动态增长的输出缓冲区，初始大小取压缩数据的2倍或8KB
        int outPos = 0; // 输出缓冲区写入位置
        int pos = 0; // 输入数据读取位置

        while (pos < data.length) { // 遍历压缩数据
            int flag = data[pos] & 0xFF; // 读取token标记，& 0xFF转为无符号整数
            pos++; // 移动读取指针

            if (flag == (TOKEN_END & 0xFF)) { // 遇到结束标记则退出
                break;
            } else if (flag == (TOKEN_LITERAL & 0xFF)) { // 原始字节：直接输出
                if (outPos >= buf.length) buf = grow(buf, outPos); // 缓冲区不足则扩容
                buf[outPos++] = data[pos++]; // 复制原始字节到输出
            } else if (flag == (TOKEN_MATCH & 0xFF)) { // 匹配引用：从已输出数据中复制
                int distance = (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8); // 读取2字节小端序距离
                int length = (data[pos + 2] & 0xFF) + MIN_MATCH; // 读取1字节长度并加回MIN_MATCH
                pos += 3; // 跳过match的3字节数据

                int srcPos = outPos - distance; // 计算复制源位置（向前回溯distance字节）
                while (outPos + length > buf.length) buf = grow(buf, outPos); // 确保缓冲区足够
                for (int i = 0; i < length; i++) { // 逐字节复制匹配数据
                    buf[outPos++] = buf[srcPos + i]; // 从已输出的位置复制（可能重叠，所以逐字节复制）
                }
            } else { // 未知token类型
                throw new RuntimeException("解压失败：未知的 token 类型 " + flag);
            }
        }

        return Arrays.copyOf(buf, outPos); // 截取有效部分返回
    }

    private static Match findLongestMatch(byte[] data, int pos) { // 在滑动窗口中查找与当前位置匹配的最长子串
        int windowStart = Math.max(0, pos - WINDOW_SIZE); // 窗口起始位置（不超出数据开头）
        int lookaheadEnd = Math.min(data.length, pos + LOOKAHEAD_SIZE); // 前瞻缓冲区结束位置（不超出数据末尾）
        int maxPossibleLen = lookaheadEnd - pos; // 最大可能匹配长度

        if (maxPossibleLen < MIN_MATCH) { // 剩余数据不足最小匹配长度则放弃
            return null;
        }

        int bestDistance = 0; // 最佳匹配的回溯距离
        int bestLength = 0; // 最佳匹配的长度

        for (int winPos = windowStart; winPos < pos; winPos++) { // 在窗口中逐位置搜索
            int matchLen = 0; // 当前位置的匹配长度
            while (matchLen < maxPossibleLen // 不超出前瞻范围
                    && (pos + matchLen) < data.length // 不超出数据末尾
                    && (winPos + matchLen) < pos // 不超出窗口边界（不能匹配到未处理数据）
                    && data[winPos + matchLen] == data[pos + matchLen]) { // 逐字节比较
                matchLen++; // 匹配成功，继续比较下一字节
            }

            if (matchLen > bestLength) { // 找到更长的匹配
                bestLength = matchLen; // 更新最佳长度
                bestDistance = pos - winPos; // 计算回溯距离
                if (bestLength == maxPossibleLen) { // 已达最大可能长度则提前终止搜索
                    break;
                }
            }
        }

        if (bestLength >= MIN_MATCH) { // 有效匹配返回结果
            return new Match(bestDistance, bestLength);
        }
        return null; // 无有效匹配
    }

    private static byte[] grow(byte[] old, int minSize) { // 缓冲区扩容：至少扩大一倍或满足最小需求
        byte[] next = new byte[Math.max(old.length * 2, minSize + 4096)]; // 新缓冲区大小取2倍或minSize+4KB
        System.arraycopy(old, 0, next, 0, Math.min(minSize, old.length)); // 复制已有数据到新缓冲区
        return next;
    }

    private static class Match { // 匹配结果：记录回溯距离和匹配长度
        final int distance; // 从当前位置向前回溯的字节数
        final int length; // 匹配的字节长度

        Match(int distance, int length) { // 构造匹配对象
            this.distance = distance;
            this.length = length;
        }
    }

}
