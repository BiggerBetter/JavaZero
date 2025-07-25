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

    /** 按层级（高→低）排列的编号正则。调用方可 setPatterns(...) 覆盖自定义。*/
    private static List<Pattern> PATTERNS = new ArrayList<>(Arrays.asList(
            // 一级：  一、
            Pattern.compile("^([一二三四五六七八九十]+)、\\s*(.+)$"),
            // 二级：  （一）
            Pattern.compile("^（([一二三四五六七八九十]+)）\\s*(.+)$"),
            // 三级：  1.
            Pattern.compile("^([0-9]+)\\.\\s*(.+)$"),
            // 四级：  （1）
            Pattern.compile("^（([0-9]+)）\\s*(.+)$")
    ));

    /** 允许调用方按需替换（支持不同模版） */
    public static void setPatterns(List<Pattern> patterns) {
        PATTERNS = patterns;
    }

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
                    if (!isReportStarted && PATTERNS.get(0).matcher(text).matches()){
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
        Node[] currents = new Node[PATTERNS.size()];   // 按层索引缓存最近节点

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int hitLevel = -1;
            Matcher hitMatcher = null;

            // ① 判定命中层级
            for (int i = 0; i < PATTERNS.size(); i++) {
                Matcher m = PATTERNS.get(i).matcher(line);
                if (m.find()) {
                    hitLevel = i + 1;          // 层级 = 索引 + 1
                    hitMatcher = m;
                    break;
                }
            }

            if (hitLevel > 0) {               // 命中了标题
                // ② 取编号 & 标题文字
                String numPart = hitMatcher.group(1);
                String titlePart = hitMatcher.group(2).trim();

                String rawNumber, numberKey;
                switch (hitLevel) {
                    case 1:  rawNumber = numPart + "、";          numberKey = rawNumber; break;
                    case 2:  rawNumber = "（" + numPart + "）";    numberKey = rawNumber; break;
                    case 3:  rawNumber = numPart + ".";           numberKey = rawNumber; break;
                    default: rawNumber = "（" + numPart + "）";    numberKey = rawNumber; break; // 四级及以后：沿用括号形式
                }

                // ③ 生成节点
                Node parent = (hitLevel == 1) ? root : currents[hitLevel - 2];
                if (parent == null) parent = root;   // 容错：孤儿标题归根
                Node node = createNode(hitLevel, rawNumber, titlePart, numberKey, parent);
                parent.children.add(node);

                // ④ 维护 currents[]
                currents[hitLevel - 1] = node;
                for (int i = hitLevel; i < currents.length; i++) currents[i] = null;

            } else {                        // 正文：归到最近的非空 currents
                Node target = root;
                for (int i = currents.length - 1; i >= 0; i--) {
                    if (currents[i] != null) {
                        target = currents[i];
                        break;
                    }
                }
                target.contents.add(line);
            }
        }
        return root;
    }

    // 构造节点并补全 key
    private static Node createNode(int level, String rawNumber, String title,
                                   String numberKey, Node parent) {
        String fullNumKey = parent.fullNumberKey.isEmpty()
                ? numberKey
                : parent.fullNumberKey + "-" + numberKey;
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

    public static Map<String, List<String>> buildTitleToNumberMap(Node root) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        traverseAndCollect(root, map);
        return map;
    }

    private static void traverseAndCollect(Node node, Map<String, List<String>> map) {
        if (node.level != 0) {
            List<String> list = map.get(node.contentKey);
            if (list == null) {
                list = new ArrayList<>();
                map.put(node.contentKey, list);
            }
            list.add(node.rawNumber);
        }
        for (Node child : node.children) {
            traverseAndCollect(child, map);
        }
    }

    // ──────────────────────────────────────────────────────────
    //  模版 → 报告：编号校正辅助
    // ──────────────────────────────────────────────────────────

    /** 1) 由模版行构造『contentKey → 标准编号』映射（每个标题取第一个编号）。*/
    public static Map<String, String> buildTemplateNumberMap(List<String> templateLines) {
        Node tmplRoot = parse(templateLines);
        Map<String, List<String>> tmp = buildTitleToNumberMap(tmplRoot);

        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : tmp.entrySet()) {
            if (!e.getValue().isEmpty()) {
                map.put(e.getKey(), e.getValue().get(0));   // 取首编号为权威值
            }
        }
        return map;
    }

    /** 2) 递归套用模版编号到报告树，若不一致则改写为模版值。*/
    private static void applyTemplateNumbers(Node node, Map<String, String> tmplNumMap) {
        if (node.level != 0) {
            String stdNum = tmplNumMap.get(node.contentKey);
            if (stdNum != null && !stdNum.equals(node.rawNumber)) {
                node.rawNumber = stdNum;
                node.numberKey = stdNum;
            }
        }
        for (Node child : node.children) {
            applyTemplateNumbers(child, tmplNumMap);
        }
    }

    /** 3) 校正后需重建 fullNumberKey，保持父子连贯。*/
    private static void rebuildFullKeys(Node node, String parentKey) {
        if (node.level == 0) {
            node.fullNumberKey = "";
        } else {
            node.fullNumberKey = parentKey.isEmpty()
                    ? node.numberKey
                    : parentKey + "-" + node.numberKey;
        }
        for (Node ch : node.children) {
            rebuildFullKeys(ch, node.fullNumberKey);
        }
    }

    /** ===== 高阶 API：传入【模版行】+【报告行】→ 得到已校正的报告树 ===== */
    public static Node parseReportWithTemplate(List<String> templateLines,
                                               List<String> reportLines) {
        Map<String, String> tmplNumMap = buildTemplateNumberMap(templateLines);
        Node reportRoot = parse(reportLines);

        applyTemplateNumbers(reportRoot, tmplNumMap);
        rebuildFullKeys(reportRoot, "");
        return reportRoot;
    }

    // ────── 示例 Main ──────
    public static void main(String[] args) throws IOException {
        // ====== 演示：模版 & 报告 双解析 ======
        String templatePath = "/Users/Jenius/Desktop/授信-模版.docx";
        String reportPath   = "/Users/Jenius/Desktop/授信-报告.docx";

        List<String> tmplLines   = extractWord(templatePath);
        List<String> reportLines = extractWord(reportPath);

        Node reportRoot = parseReportWithTemplate(tmplLines, reportLines);

        // 打印校正后大纲
        System.out.println("===== 纠正后大纲 =====");
        printTree(reportRoot, "");

        // 按需提取
        String key = "授信申请方案-上年度批复情况";
        List<String> section = extractSection(reportRoot, key);

        System.out.println("\n===== 提取结果 =====");
        for (String line : section) System.out.println(line);
    }
}