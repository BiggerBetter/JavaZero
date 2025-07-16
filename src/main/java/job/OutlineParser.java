package job;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;

import java.io.FileInputStream;
import java.io.IOException;
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

    /**
     * 从目标路径的文档中提取段落
     * @param filePath
     * @return
     * @throws IOException
     */
    public static List<String> extractWord(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            boolean isReportStarted = false;

            //遍历文档中的所有元素，对表格抽取内容，对段落抽取分段标记
            List<IBodyElement> bodyElements = document.getBodyElements();
            for (IBodyElement element : bodyElements) {
                if (element.getElementType() == BodyElementType.PARAGRAPH) {
                    XWPFParagraph para = (XWPFParagraph) element;
                    String text = para.getText().trim();
                    if (text.isEmpty()) continue;

                    //判断报告文档正文是否开始，一般以一、开始
                    if (!isReportStarted && P_L1.matcher(text).matches()){
                        isReportStarted = true;
                    }else if (!isReportStarted){
                        continue;
                    }
                    lines.add(text);
                } else if (element.getElementType() == BodyElementType.TABLE ) {
                    XWPFTable table = (XWPFTable) element;
                    // 拿到表格后，如果当前有积累段落内容，就将表格形成的文本追加进去
                    lines.add(parseTableToMarkdown(table));
                }
            }
        }

        return lines;
    }


    /**
     * 把 Word 表格序列化为“| cell | … |” 形式的 Markdown ⾏。
     *
     * <p><b>设计目标</b></p>
     * <ul>
     *   <li>⽆需输出 <code>| --- | --- |</code> 分割线，方便后续⼤模型按“|”拆分。</li>
     *   <li>任何横向（<code>gridSpan</code>）或纵向（<code>vMerge</code>）合并的单元格，都要在
     *       结果中按<strong>合并后内容填充到每个被合并位置</strong>，保证列数固定。</li>
     * </ul>
     *
     * <p><b>算法流程</b></p>
     * <ol>
     *   <li>逐⾏遍历表格。</li>
     *   <li>对每个单元格：
     *       <ul>
     *         <li>解析 <code>vMerge</code> ── 判断是否为“restart / continue”。</li>
     *         <li>解析 <code>gridSpan</code> ── 获取横向跨度。</li>
     *         <li>延续⾏：从缓存 <code>vMergeCache</code> 获取起始⾏的文本。</li>
     *         <li>起始⾏：把⽂本写⼊缓存中，对应列后续⾏可继承。</li>
     *         <li>把⽂本按 <code>gridSpan</code> 次数 push 到⽬前⾏的 <code>expanded</code> 列表。</li>
     *       </ul>
     *   </li>
     *   <li>将 <code>expanded</code> 中的所有单元格⽂本拼接输出为⼀⾏ Markdown。</li>
     * </ol>
     *
     * <p><b>关键数据结构</b></p>
     * <pre>
     * Map&lt;Integer, String&gt; vMergeCache
     *   key   ─ 合并列索引
     *   value ─ 合并起始⾏的单元格⽂本
     * </pre>
     *
     * <p>这样做的⽐较值：</p>
     * <ul>
     *   <li>逻辑简单，⽆需预扫描全表列数。</li>
     *   <li>横纵向合并可叠加，能⽀持任意矩形块。</li>
     * </ul>
     */
    private static String parseTableToMarkdown(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
        List<XWPFTableRow> rows = table.getRows();

        // key = 列索引；value = 该列中“纵向合并起始行”的文本
        Map<Integer, String> vMergeCache = new HashMap<>();

        for (XWPFTableRow row : rows) {
            // 每次处理一整行，expanded 用来保存“铺平”后的单元格文本
            List<String> expanded = new ArrayList<>();
            int colIndex = 0;

            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = cell.getText().replaceAll("\n", " ").trim();

                // ------------- Step 1: 纵向合并判定 -------------

                boolean vRestart = false;
                boolean vContinue = false;
                if (cell.getCTTc().getTcPr() != null && cell.getCTTc().getTcPr().isSetVMerge()) {
                    CTVMerge vm = cell.getCTTc().getTcPr().getVMerge();
                    String mergeVal = (vm.isSetVal() && vm.getVal() != null)
                            ? vm.getVal().toString()
                            : null;

                    if ("restart".equalsIgnoreCase(mergeVal)) {
                        vRestart = true;           // 垂直合并起始行
                    } else {                      // 包括 "continue" 或无 val
                        vContinue = true;         // 垂直合并延续行
                    }
                }
                if (vContinue) {
                    cellText = vMergeCache.getOrDefault(colIndex, "");
                }

                // ------------- Step 2: 横向合并判定 -------------
                int gridSpan = 1;
                if (cell.getCTTc().getTcPr() != null && cell.getCTTc().getTcPr().isSetGridSpan()) {
                    gridSpan = cell.getCTTc().getTcPr().getGridSpan().getVal().intValue();
                }

                // 如果是垂直合并的起始行，把文本写入缓存
                if (vRestart) {
                    for (int i = 0; i < gridSpan; i++) {
                        vMergeCache.put(colIndex + i, cellText);
                    }
                }

                // 横向复制
                for (int i = 0; i < gridSpan; i++) {
                    expanded.add(cellText);
                    colIndex++;
                }
            }

            // ------------- Step 3: 输出本行 -------------
            for (String txt : expanded) {
                sb.append("| ").append(txt).append(" ");
            }
            sb.append("|\n");
        }

        return sb.toString();
    }

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
                        m1.group(1)+ "、", root);
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
                        numKey+ ".", currentL2);
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
        String fullNumKey = parent.fullNumberKey + "/"+ numberKey;
        String contentKey = parent.level == 0 ? title
                : parent.contentKey + "-" + title;
        return new Node(level, rawNumber, title, numberKey, fullNumKey, contentKey);
    }

    // ────── 遍历输出（深度优先，含正文）──────
    public static void printTree(Node node, String indent) {
        if (node.level != 0) { // 跳过虚根
            System.out.printf("%s[%s] -- [%s]%n", indent, "标题序号Key："+node.fullNumberKey, "标题内容Key："+node.contentKey);
            for (String body : node.contents) {
                System.out.printf("%s    └─ %s%n", indent, body);
            }
        }
        for (Node child : node.children) {
            printTree(child, indent + "    ");
        }
    }

    /** ==== 1) 递归查找：根据 contentKey 精确匹配 ==== */
    public static Node findByContentKey(Node node, String key) {
        if (key.equals(node.contentKey)) return node;
        for (Node child : node.children) {
            Node hit = findByContentKey(child, key);
            if (hit != null) return hit;
        }
        return null;    // 未找到
    }

    /** ==== 2) 提取并还原整段文本（含标题与正文） ==== */
    public static List<String> extractSection(Node root, String contentKey) {
        Node target = findByContentKey(root, contentKey);
        if (target == null) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        buildLines(target, 0, lines);      // 从目标节点向下收集
        return lines;
    }

    /** 辅助：递归组装“标题行 + 正文行”，并做缩进 */
    private static void buildLines(Node node, int indentLv, List<String> out) {
        if (node.level != 0) {             // 虚根除外
            String indent = repeat("    ", indentLv);   // 4 空格 / 级
            out.add(indent + node.rawNumber + " " + node.title);
            for (String para : node.contents) {
                out.add(indent + "    " + para);        // 正文再缩一层
            }
        }
        for (Node child : node.children) {
            buildLines(child, indentLv + 1, out);
        }
    }

    /** 简易 String repeat（兼容 Java 8） */
    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder(s.length() * times);
        for (int i = 0; i < times; i++) sb.append(s);
        return sb.toString();
    }


    // ────── 示例 Main ──────
    public static void main(String[] args) throws IOException {
        List<String> sample = Arrays.asList(
                "一、授信申请方案",
                "（一）基本情况",
                "1. 贷款主体",
                "贷款主体为 XX 有限公司，成立于……",
                "贷款申请人为 XX ，出生于……",
                "贷款时间为 XXxx",
                "2. 担保方案",
                "担保人1：YY 实业…",
                "担保人2：ZZ 实业…",
                "（二）上年度批复情况",
                "1. 批复额度",
                "2023 年批复总额为……",
                "2024 年批复总额为……",
                "2. 实际使用余额",
                "截至 2025-05-30 使用余额……",
                "截至 2025-06-30 使用余额……",
                "二、风险评估",
                "（一）行业分析",
                "1. 行业趋势",
                "行业整体增长…",
                "2. 政策影响",
                "最新政策……"
        );

        String filePath = "/Users/Jenius/Desktop/分段测试.docx";
        // sample = extractWord(filePath);
        // for (String tmp :sample){
        //     System.out.println(tmp);
        // }
        Node root = parse(sample);

        printTree(root, "");

        String key = "授信申请方案-上年度批复情况";          // 也可以用 "授信申请方案"
        List<String> section = extractSection(root, key);

        // ③ 打印查看
        System.out.println("\n===== 提取结果 =====");
        for (String line : section) System.out.println(line);
    }
}