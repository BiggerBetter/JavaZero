package Utils;

import org.apache.poi.xwpf.usermodel.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

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

    //将表格转成marketDown格式内容
    private static String parseTableToMarkdown(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
        List<XWPFTableRow> rows = table.getRows();
        for (int i = 0; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<XWPFTableCell> cells = row.getTableCells();
            for (XWPFTableCell cell : cells) {
                sb.append("| ").append(cell.getText().replaceAll("\n", " ")).append(" ");
            }
            sb.append("|\n");
        }
        return sb.toString();
    }
}