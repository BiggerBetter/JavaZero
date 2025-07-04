package Utils;

import org.apache.poi.xwpf.usermodel.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;

public class WordSplitMethod {

    // 匹配类似“（一）”、“（二）”、“（三）...”的段落标题
    private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile("（[一二三四五六七八九十]+）.*");

    public static void main(String[] args) throws IOException {
        String filePath = "/Users/Jenius/Desktop/分段测试.docx";

        Map<String, List<String>> sections = extractSections(filePath);

        // 打印每个部分的内容
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            System.out.println("=== Section: " + entry.getKey() + " ===");
            for (String para : entry.getValue()) {
                System.out.println(para);
            }
            System.out.println();
        }
    }

    /**
     * 从目标路径的文档中提取段落
     * @param filePath
     * @return
     * @throws IOException
     */
    public static Map<String, List<String>> extractSections(String filePath) throws IOException {
        Map<String, List<String>> sectionMap = new LinkedHashMap<>();
        String currentSection = null;

        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            //遍历文档中的所有元素，对表格抽取内容，对段落抽取分段标记
            List<IBodyElement> bodyElements = document.getBodyElements();
            for (IBodyElement element : bodyElements) {
                if (element.getElementType() == BodyElementType.PARAGRAPH) {
                    XWPFParagraph para = (XWPFParagraph) element;
                    String text = para.getText().trim();
                    if (text.isEmpty()) continue;

                    Matcher matcher = SECTION_HEADER_PATTERN.matcher(text);
                    // 如果找到分段标记，就将当前的txt放入map中
                    if (matcher.matches()) {
                        currentSection = text;
                        sectionMap.put(currentSection, new ArrayList<>());
                    } else if (currentSection != null) {
                        sectionMap.get(currentSection).add(text);
                    }
                } else if (element.getElementType() == BodyElementType.TABLE && currentSection != null) {
                    XWPFTable table = (XWPFTable) element;
                    // 拿到表格后，如果当前有积累段落内容，就将表格形成的文本追加进去
                    sectionMap.get(currentSection).add(parseTableToMarkdown(table));
                }
            }
        }

        return sectionMap;
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
}