package job;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 3 级中文编号大纲，并生成两类 key。
 * 兼容 Java 8，尽量不用生僻语法。
 */
public class OutlineParser {

    // ────── 树节点 ──────
    static class Node {
        int level;                     // 1/2/3
        String rawNumber;              // 原编号（含顿号/点号/括号）
        String title;                  // 纯标题文字
        String numberKey;              // 加工后的编号（本级）
        String fullNumberKey;          // 从根到本级的编号串
        String contentKey;             // 从根到本级的标题文字串
        List<String> contents = new ArrayList<>();   // 正文段落
        List<Node> children = new ArrayList<>();

        Node(int level, String rawNumber, String title,
             String numberKey, String fullNumberKey, String contentKey) {
            this.level = level;
            this.rawNumber = rawNumber;
            this.title = title;
            this.numberKey = numberKey;
            this.fullNumberKey = fullNumberKey;
            this.contentKey = contentKey;
        }
    }

    // ────── 正则（全角“一、”“（一）”；半角“1.”）──────
    private static final Pattern P_L1 = Pattern.compile("^([一二三四五六七八九十]+)、\\s*(.+)$");
    private static final Pattern P_L2 = Pattern.compile("^（([一二三四五六七八九十]+)）\\s*(.+)$");
    private static final Pattern P_L3 = Pattern.compile("^([0-9]+)\\.\\s*(.+)$");

    // 解析入口
    public static Node parse(List<String> lines) {
        Node root = new Node(0, "", "ROOT", "", "", ""); // 虚根
        Node currentL1 = null;
        Node currentL2 = null;
        Node currentL3 = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher m1 = P_L1.matcher(line);
            Matcher m2 = P_L2.matcher(line);
            Matcher m3 = P_L3.matcher(line);

            if (m1.find()) {                 // 一级
                currentL1 = createNode(1, m1.group(1) + "、", m1.group(2),
                        m1.group(1), root);
                root.children.add(currentL1);
                currentL2 = currentL3 = null;
            } else if (m2.find()) {          // 二级
                if (currentL1 == null) continue; // 容错：忽略孤儿
                String numKey = "（" + m2.group(1) + "）";
                currentL2 = createNode(2, numKey, m2.group(2),
                        numKey, currentL1);
                currentL1.children.add(currentL2);
                currentL3 = null;
            } else if (m3.find()) {          // 三级
                if (currentL2 == null) continue;
                String numKey = m3.group(1); // 去掉点号
                currentL3 = createNode(3, m3.group(1) + ".", m3.group(2),
                        numKey, currentL2);
                currentL2.children.add(currentL3);
            } else {                         // 正文
                // 归到最近的标题节点；如果三级存在，就放三级；否则二级，再否则一级
                Node target = currentL3 != null ? currentL3
                        : currentL2 != null ? currentL2
                        : currentL1 != null ? currentL1
                        : root;
                target.contents.add(line);
            }
        }
        return root;
    }

    // 构造节点并补全 key
    private static Node createNode(int level, String rawNumber, String title,
                                   String numberKey, Node parent) {
        String fullNumKey = parent.fullNumberKey + numberKey;
        String contentKey = parent.level == 0 ? title
                : parent.contentKey + "-" + title;
        return new Node(level, rawNumber, title, numberKey, fullNumKey, contentKey);
    }

    // ────── 遍历输出（深度优先，含正文）──────
    public static void printTree(Node node, String indent) {
        if (node.level != 0) { // 跳过虚根
            System.out.printf("%s[%s]  %s%n",
                    indent, node.fullNumberKey, node.contentKey);
            for (String body : node.contents) {
                System.out.printf("%s    └─ %s%n", indent, body);
            }
        }
        for (Node child : node.children) {
            printTree(child, indent + "    ");
        }
    }

    // ────── 示例 Main ──────
    public static void main(String[] args) {
        List<String> sample = Arrays.asList(
                "附件1",
                "关注信息",
                "一、授信申请方案1",
                "（一）基本情况",
                "1. 贷款主体",
                "贷款主体为 XX 有限公司，成立于……",
                "2. 担保方案",
                "担保人：YY 实业…",
                "（二）上年度批复情况",
                "1. 批复额度",
                "2024 年批复总额为……",
                "2. 实际使用余额",
                "截至 2025-06-30 使用余额……",
                "二、风险评估",
                "（一）行业分析",
                "1. 行业趋势",
                "行业整体增长…",
                "2. 政策影响",
                "最新政策……"
        );

        Node root = parse(sample);
        printTree(root, "");
    }
}