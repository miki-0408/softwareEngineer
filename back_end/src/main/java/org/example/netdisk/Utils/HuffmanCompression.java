package org.example.netdisk.Utils;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class HuffmanCompression {

    private static final int SYMBOL_COUNT = 256; // 所有可能的字节值（0-255），覆盖全部字节

    public static byte[] compress(byte[] data) { // 哈夫曼压缩：统计频率→建树→编码→打包输出
        if (data == null || data.length == 0) return new byte[0]; // 空数据不压缩

        int[] freq = new int[SYMBOL_COUNT]; // 频率表，统计每个字节值出现次数
        for (byte b : data) freq[b & 0xFF]++; // 遍历数据统计频率，& 0xFF将byte转为无符号整数

        Node root = buildTree(freq); // 根据频率表构建哈夫曼树
        if (root == null) return new byte[0]; // 频率表全空则无法压缩

        String[] codes = new String[SYMBOL_COUNT]; // 编码表，存储每个字节值对应的哈夫曼编码字符串
        generateCodes(root, "", codes); // 遍历哈夫曼树生成编码表

        StringBuilder encoded = new StringBuilder(); // 拼接所有字节的编码
        for (byte b : data) {
            encoded.append(codes[b & 0xFF]); // 将每个字节替换为其哈夫曼编码
        }
        int bitLen = encoded.length(); // 编码后的总位数
        byte[] encodedBytes = bitsToBytes(encoded.toString()); // 将01字符串转为紧凑的字节数组

        ByteArrayOutputStream bos = new ByteArrayOutputStream(); // 输出流，构建压缩数据
        bos.write((data.length >> 24) & 0xFF); // 写入原始数据长度（高8位）
        bos.write((data.length >> 16) & 0xFF); // 写入原始数据长度（次高8位）
        bos.write((data.length >> 8) & 0xFF); // 写入原始数据长度（次低8位）
        bos.write(data.length & 0xFF); // 写入原始数据长度（低8位）
        for (int f : freq) { // 写入256个整数的频率表，解压时重建哈夫曼树
            bos.write((f >> 24) & 0xFF); // 频率值高8位
            bos.write((f >> 16) & 0xFF); // 频率值次高8位
            bos.write((f >> 8) & 0xFF); // 频率值次低8位
            bos.write(f & 0xFF); // 频率值低8位
        }
        bos.write((bitLen >> 24) & 0xFF); // 写入编码位数（高8位），解压时确定有效位
        bos.write((bitLen >> 16) & 0xFF); // 编码位数次高8位
        bos.write((bitLen >> 8) & 0xFF); // 编码位数次低8位
        bos.write(bitLen & 0xFF); // 编码位数低8位
        try { bos.write(encodedBytes); } catch (Exception e) { throw new RuntimeException("编码写入失败", e); } // 写入压缩后的字节数据

        return bos.toByteArray(); // 返回完整压缩包：[原始长度][频率表][位数][编码数据]
    }

    public static byte[] decompress(byte[] data) { // 哈夫曼解压：读取头部→重建树→解码还原
        if (data == null || data.length <= 1028) return new byte[0]; // 数据小于最小头部大小（4+1024+0）则无效

        int pos = 0; // 当前读取位置指针
        int origLen = ((data[pos] & 0xFF) << 24) | ((data[pos+1] & 0xFF) << 16) // 读取4字节原始数据长度
                    | ((data[pos+2] & 0xFF) << 8) | (data[pos+3] & 0xFF);
        pos += 4; // 跳过长度字段

        int[] freq = new int[SYMBOL_COUNT]; // 重建频率表
        for (int i = 0; i < SYMBOL_COUNT; i++) { // 读取256个频率值
            freq[i] = ((data[pos] & 0xFF) << 24) | ((data[pos+1] & 0xFF) << 16) // 读取4字节频率
                    | ((data[pos+2] & 0xFF) << 8) | (data[pos+3] & 0xFF);
            pos += 4; // 跳过当前频率值
        }

        int bitLen = ((data[pos] & 0xFF) << 24) | ((data[pos+1] & 0xFF) << 16) // 读取4字节编码位数
                   | ((data[pos+2] & 0xFF) << 8) | (data[pos+3] & 0xFF);
        pos += 4; // 跳过位数字段

        Node root = buildTree(freq); // 用频率表重建完全相同的哈夫曼树
        if (root == null) return new byte[0]; // 树重建失败

        byte[] encodedBytes = Arrays.copyOfRange(data, pos, data.length); // 提取编码后的字节数据
        String bits = bytesToBits(encodedBytes, bitLen); // 将字节数组还原为01字符串

        byte[] result = new byte[origLen]; // 解压结果数组
        int outPos = 0; // 输出位置
        Node current = root; // 从根节点开始遍历
        for (int i = 0; i < bits.length() && outPos < origLen; i++) { // 逐位遍历编码字符串
            current = (bits.charAt(i) == '0') ? current.left : current.right; // 0走左子树，1走右子树
            if (current != null && current.left == null && current.right == null) { // 到达叶子节点即找到一个编码
                result[outPos++] = (byte) current.value; // 输出叶子节点对应的字节值
                current = root; // 回到根节点继续解码下一个字符
            }
        }
        return result; // 返回解压后的原始数据
    }

    private static Node buildTree(int[] freq) { // 用最小堆构建哈夫曼树：每次取频率最小的两个节点合并
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.freq)); // 最小堆，按频率升序
        for (int i = 0; i < SYMBOL_COUNT; i++) { // 将所有出现的字符作为叶子节点入堆
            if (freq[i] > 0) pq.add(new Node(i, freq[i])); // 仅添加频率大于0的字节值
        }
        if (pq.isEmpty()) return null; // 无任何字符则返回null
        if (pq.size() == 1) { // 只有一种字符时创建虚拟父节点，确保编码非空
            Node only = pq.poll(); // 取出唯一节点
            Node dummy = new Node(-1, only.freq); // 创建内部节点（value=-1表示非叶子）
            dummy.left = only; // 唯一节点作为左子节点
            return dummy; // 返回虚拟根节点
        }
        while (pq.size() > 1) { // 循环合并直到只剩一个根节点
            Node left = pq.poll(); // 取出频率最小的节点
            Node right = pq.poll(); // 取出频率第二小的节点
            Node parent = new Node(-1, left.freq + right.freq); // 创建父节点，频率为子节点之和
            parent.left = left; // 设置左子节点
            parent.right = right; // 设置右子节点
            pq.add(parent); // 父节点重新入堆
        }
        return pq.poll(); // 返回哈夫曼树的根节点
    }

    private static void generateCodes(Node node, String code, String[] codes) { // 递归遍历哈夫曼树生成编码表
        if (node == null) return; // 空节点直接返回
        if (node.left == null && node.right == null) { // 叶子节点：获得完整编码
            codes[node.value] = code.isEmpty() ? "0" : code; // 若只有一种字符时编码为"0"
            return;
        }
        generateCodes(node.left, code + "0", codes); // 左子树追加'0'继续递归
        generateCodes(node.right, code + "1", codes); // 右子树追加'1'继续递归
    }

    private static byte[] bitsToBytes(String bits) { // 将01字符串打包为字节数组，节省存储空间
        int byteCount = (bits.length() + 7) / 8; // 向上取整计算所需字节数
        byte[] result = new byte[byteCount]; // 结果字节数组
        for (int i = 0; i < bits.length(); i++) { // 遍历每个位
            if (bits.charAt(i) == '1') { // 仅处理值为'1'的位
                result[i / 8] |= (1 << (7 - (i % 8))); // 在对应字节对应位置置1（大端序：高位在前）
            }
        }
        return result;
    }

    private static String bytesToBits(byte[] bytes, int bitCount) { // 从字节数组中按位还原01字符串
        StringBuilder sb = new StringBuilder(); // 字符串构建器
        for (int i = 0; i < bitCount; i++) { // 只提取指定位数
            int bit = (bytes[i / 8] >> (7 - (i % 8))) & 1; // 提取对应字节对应位的值（大端序）
            sb.append(bit); // 追加位值
        }
        return sb.toString(); // 返回01字符串
    }

    private static class Node { // 哈夫曼树节点
        final int value; // 字节值（0-255），-1表示内部节点
        final int freq; // 该节点代表的频率之和
        Node left, right; // 左右子节点

        Node(int value, int freq) { // 构造节点
            this.value = value;
            this.freq = freq;
        }
    }
}
