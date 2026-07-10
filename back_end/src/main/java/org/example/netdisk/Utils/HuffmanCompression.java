package org.example.netdisk.Utils;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * 手动实现的哈夫曼编码压缩
 *
 * 原理：
 *   1. 统计字节频率
 *   2. 构建哈夫曼树（最小堆 + 贪心合并）
 *   3. 生成变长编码表（高频字节用短码）
 *   4. 编码 → 压缩，解码 → 还原
 *
 * 输出格式：
 *   [4B 原始长度] [256×4B 频率表] [4B 编码后位数] [变长比特流...]
 */
public class HuffmanCompression {

    private static final int SYMBOL_COUNT = 256; // 所有可能的字节值

    /** 压缩 */
    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) return new byte[0];

        // 1. 统计频率
        int[] freq = new int[SYMBOL_COUNT];
        for (byte b : data) freq[b & 0xFF]++;

        // 2. 构建哈夫曼树
        Node root = buildTree(freq);
        if (root == null) return new byte[0];

        // 3. 生成编码表
        String[] codes = new String[SYMBOL_COUNT];
        generateCodes(root, "", codes);

        // 4. 编码数据
        StringBuilder encoded = new StringBuilder();
        for (byte b : data) {
            encoded.append(codes[b & 0xFF]);
        }
        int bitLen = encoded.length();
        byte[] encodedBytes = bitsToBytes(encoded.toString());

        // 5. 写入头部 + 数据
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 原始长度 (4B)
        bos.write((data.length >> 24) & 0xFF);
        bos.write((data.length >> 16) & 0xFF);
        bos.write((data.length >> 8) & 0xFF);
        bos.write(data.length & 0xFF);
        // 频率表 (256×4B)
        for (int f : freq) {
            bos.write((f >> 24) & 0xFF);
            bos.write((f >> 16) & 0xFF);
            bos.write((f >> 8) & 0xFF);
            bos.write(f & 0xFF);
        }
        // 编码位数 (4B)
        bos.write((bitLen >> 24) & 0xFF);
        bos.write((bitLen >> 16) & 0xFF);
        bos.write((bitLen >> 8) & 0xFF);
        bos.write(bitLen & 0xFF);
        // 编码数据
        try { bos.write(encodedBytes); } catch (Exception e) { throw new RuntimeException("编码写入失败", e); }

        return bos.toByteArray();
    }

    /** 解压 */
    public static byte[] decompress(byte[] data) {
        if (data == null || data.length <= 1028) return new byte[0];

        int pos = 0;
        // 读取原始长度
        int origLen = ((data[pos] & 0xFF) << 24) | ((data[pos+1] & 0xFF) << 16)
                    | ((data[pos+2] & 0xFF) << 8) | (data[pos+3] & 0xFF);
        pos += 4;

        // 读取频率表
        int[] freq = new int[SYMBOL_COUNT];
        for (int i = 0; i < SYMBOL_COUNT; i++) {
            freq[i] = ((data[pos] & 0xFF) << 24) | ((data[pos+1] & 0xFF) << 16)
                    | ((data[pos+2] & 0xFF) << 8) | (data[pos+3] & 0xFF);
            pos += 4;
        }

        // 读取编码位数
        int bitLen = ((data[pos] & 0xFF) << 24) | ((data[pos+1] & 0xFF) << 16)
                   | ((data[pos+2] & 0xFF) << 8) | (data[pos+3] & 0xFF);
        pos += 4;

        // 重建哈夫曼树
        Node root = buildTree(freq);
        if (root == null) return new byte[0];

        // 读取编码数据并解码
        byte[] encodedBytes = Arrays.copyOfRange(data, pos, data.length);
        String bits = bytesToBits(encodedBytes, bitLen);

        byte[] result = new byte[origLen];
        int outPos = 0;
        Node current = root;
        for (int i = 0; i < bits.length() && outPos < origLen; i++) {
            current = (bits.charAt(i) == '0') ? current.left : current.right;
            if (current != null && current.left == null && current.right == null) {
                result[outPos++] = (byte) current.value;
                current = root;
            }
        }
        return result;
    }

    // ==================== 哈夫曼树 ====================

    private static Node buildTree(int[] freq) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.freq));
        for (int i = 0; i < SYMBOL_COUNT; i++) {
            if (freq[i] > 0) pq.add(new Node(i, freq[i]));
        }
        if (pq.isEmpty()) return null;
        // 特殊情况：只有一种字符
        if (pq.size() == 1) {
            Node only = pq.poll();
            Node dummy = new Node(-1, only.freq);
            dummy.left = only;
            return dummy;
        }
        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            Node parent = new Node(-1, left.freq + right.freq);
            parent.left = left;
            parent.right = right;
            pq.add(parent);
        }
        return pq.poll();
    }

    private static void generateCodes(Node node, String code, String[] codes) {
        if (node == null) return;
        if (node.left == null && node.right == null) {
            codes[node.value] = code.isEmpty() ? "0" : code;
            return;
        }
        generateCodes(node.left, code + "0", codes);
        generateCodes(node.right, code + "1", codes);
    }

    // ==================== 位操作 ====================

    private static byte[] bitsToBytes(String bits) {
        int byteCount = (bits.length() + 7) / 8;
        byte[] result = new byte[byteCount];
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1') {
                result[i / 8] |= (1 << (7 - (i % 8)));
            }
        }
        return result;
    }

    private static String bytesToBits(byte[] bytes, int bitCount) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bitCount; i++) {
            int bit = (bytes[i / 8] >> (7 - (i % 8))) & 1;
            sb.append(bit);
        }
        return sb.toString();
    }

    // ==================== 树节点 ====================

    private static class Node {
        final int value;  // 字节值（-1 表示内部节点）
        final int freq;   // 频率
        Node left, right;

        Node(int value, int freq) {
            this.value = value;
            this.freq = freq;
        }
    }
}
