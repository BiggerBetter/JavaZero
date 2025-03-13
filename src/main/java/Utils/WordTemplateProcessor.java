package Utils;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordTemplateProcessor {

    public static void main(String[] args) throws Exception {
        // 模板文件路径与输出文件路径
        String templatePath = "/Users/Jenius/Desktop/格式测试模版.docx";
        String outputPath = "/Users/Jenius/Desktop/output.docx";

        // 填充数据
        Map<String, String> data = new HashMap<>();
        data.put("name", "张三");
        data.put("department", "技术部");
        data.put("report_date", "2023-10-01");
        data.put("sales_data", "销售额表：\n| 产品 | 销量 |\n|----|----|\n| 手机 | 1000 |\n| 电脑 | 500 |");

        // 读取模板文档
        InputStream is = new FileInputStream(templatePath);
        XWPFDocument doc = new XWPFDocument(is);

        // 处理正文中的段落
        processParagraphs(doc.getParagraphs(), doc, data);

        // 处理文档中所有表格内的段落（注意：这里不处理嵌入的 Excel，只处理 Word 内嵌的表格）
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    processParagraphs(cell.getParagraphs(), doc, data);
                }
            }
        }

        // 保存处理后的文档
        FileOutputStream fos = new FileOutputStream(outputPath);
        doc.write(fos);
        fos.close();
        doc.close();
        is.close();
        System.out.println("文档处理完成，输出文件：" + outputPath);
    }

    /**
     * 处理一个段落列表，查找并替换占位符。
     * 替换时，如果遇到表格型数据则会将原段落拆分，新建段落和表格插入。
     */
    private static void processParagraphs(List<XWPFParagraph> paragraphs, XWPFDocument doc, Map<String, String> data) {
        // 逐个段落处理（正向遍历即可）
        List<XWPFParagraph> paragraphsCopy = new ArrayList<>(paragraphs);
        for (XWPFParagraph para : paragraphsCopy) {
            String paraText = para.getText();
            if (paraText.contains("${")) {
                // 将该段落拆分成若干段：普通文本段和占位符段
                List<Segment> segments = parseSegments(paraText, data);
                // 用于后续重新构造段落的当前段落，初始即为原段落
                XWPFParagraph currentPara = para;
                // 清除原有所有 run（后面重新添加）
                int runCount = currentPara.getRuns().size();
                for (int i = runCount - 1; i >= 0; i--) {
                    currentPara.removeRun(i);
                }
                boolean firstSegment = true;
                for (Segment seg : segments) {
                    if (!seg.isPlaceholder) {
                        // 普通文本：如果是首段则在当前段落添加，否则新建段落
                        if (firstSegment) {
                            currentPara.createRun().setText(seg.text);
                            firstSegment = false;
                        } else {
                            XWPFParagraph newPara = doc.insertNewParagraph(currentPara.getCTP().newCursor());
                            newPara.createRun().setText(seg.text);
                            currentPara = newPara;
                        }
                    } else {
                        // 占位符段
                        String replacement = seg.replacement.replacement;
                        if (isTableContent(replacement)) {
                            // 处理表格型数据：
                            // 判断是否存在标题（第一行非表格行，后续行为表格）
                            String[] lines = replacement.split("\n");
                            String title = "";
                            int tableStartIndex = 0;
                            if (lines.length > 0 && !lines[0].trim().startsWith("|")) {
                                title = lines[0].trim();
                                tableStartIndex = 1;
                            }
                            if (!title.isEmpty()) {
                                // 如果当前段落为空，则直接写标题，否则另起一段
                                if (firstSegment) {
                                    currentPara.createRun().setText(title);
                                    firstSegment = false;
                                } else {
                                    XWPFParagraph titlePara = doc.insertNewParagraph(currentPara.getCTP().newCursor());
                                    titlePara.createRun().setText(title);
                                    currentPara = titlePara;
                                }
                            }
                            // 收集表格 markdown 行（忽略空行）
                            List<String> tableLines = new ArrayList<>();
                            for (int idx = tableStartIndex; idx < lines.length; idx++) {
                                String line = lines[idx].trim();
                                if (!line.isEmpty()) {
                                    tableLines.add(line);
                                }
                            }
                            // 新建表格（插入位置在当前段落后面）
                            XWPFTable table = doc.insertNewTbl(currentPara.getCTP().newCursor());
                            // 如果至少有两行（第一行为标题，第二行为分隔符），则按 markdown 格式解析
                            if (tableLines.size() >= 2) {
                                // 第一行为表头，第二行为分隔符，后续为数据行
                                String headerLine = tableLines.get(0);
                                List<String> headers = parseMarkdownRow(headerLine);
                                int colCount = headers.size();
                                // 默认表格已经创建一行，填充表头单元格
                                XWPFTableRow headerRow = table.getRow(0);
                                // 若单元格数量不足则补充
                                for (int c = headerRow.getTableCells().size(); c < colCount; c++) {
                                    headerRow.addNewTableCell();
                                }
                                List<XWPFTableCell> headerCells = headerRow.getTableCells();
                                for (int c = 0; c < colCount; c++) {
                                    headerCells.get(c).setText(headers.get(c));
                                }
                                // 数据行：从第三行开始（跳过分隔行）
                                for (int r = 2; r < tableLines.size(); r++) {
                                    List<String> cells = parseMarkdownRow(tableLines.get(r));
                                    XWPFTableRow row = table.createRow();
                                    // 这里假定列数与表头一致，多余或不足都处理
                                    for (int c = 0; c < colCount; c++) {
                                        if (c < cells.size()) {
                                            row.getCell(c).setText(cells.get(c));
                                        } else {
                                            row.getCell(c).setText("");
                                        }
                                    }
                                }
                            }
                            // 表格插入后，新建一个空段落，作为后续内容的承载
                            XWPFParagraph newPara = doc.insertNewParagraph(table.getCTTbl().newCursor());
                            currentPara = newPara;
                        } else {
                            // 普通文本替换
                            if (firstSegment) {
                                currentPara.createRun().setText(replacement);
                                firstSegment = false;
                            } else {
                                XWPFParagraph newPara = doc.insertNewParagraph(currentPara.getCTP().newCursor());
                                newPara.createRun().setText(replacement);
                                currentPara = newPara;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 解析一段文字，将其拆分为普通文本段和占位符段。
     * 占位符格式为 ${key}，其中 key 用于在 data 中查找替换内容。
     */
    private static List<Segment> parseSegments(String text, Map<String, String> data) {
        List<Segment> segments = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        while (matcher.find()) {
            // 添加占位符前的普通文本
            if (matcher.start() > lastIndex) {
                segments.add(new Segment(text.substring(lastIndex, matcher.start()), false, null));
            }
            String key = matcher.group(1);
            String replacement = data.get(key);
            if (replacement == null) {
                replacement = "";
            }
            boolean isTable = isTableContent(replacement);
            segments.add(new Segment(null, true, new Replacement(key, replacement, isTable)));
            lastIndex = matcher.end();
        }
        if (lastIndex < text.length()) {
            segments.add(new Segment(text.substring(lastIndex), false, null));
        }
        return segments;
    }

    /**
     * 判断一个替换内容是否包含表格信息：
     * 只要内容中存在换行，并且其中某行以竖线开头且以竖线结尾，就认为是表格信息。
     */
    private static boolean isTableContent(String content) {
        if (content.contains("\n")) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("|") && line.endsWith("|")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 解析 Markdown 格式的表格行，将其按竖线拆分为各个单元格的文本。
     */
    private static List<String> parseMarkdownRow(String line) {
        List<String> cells = new ArrayList<>();
        line = line.trim();
        if (line.startsWith("|")) {
            line = line.substring(1);
        }
        if (line.endsWith("|")) {
            line = line.substring(0, line.length() - 1);
        }
        String[] parts = line.split("\\|");
        for (String part : parts) {
            cells.add(part.trim());
        }
        return cells;
    }

    // 内部辅助类：用于表示一段文本的片段，可以是普通文本或占位符
    private static class Segment {
        boolean isPlaceholder;
        String text;
        Replacement replacement;

        public Segment(String text, boolean isPlaceholder, Replacement replacement) {
            this.text = text;
            this.isPlaceholder = isPlaceholder;
            this.replacement = replacement;
        }
    }

    // 内部辅助类：占位符对应的替换信息
    private static class Replacement {
        String key;
        String replacement;
        boolean isTable;

        public Replacement(String key, String replacement, boolean isTable) {
            this.key = key;
            this.replacement = replacement;
            this.isTable = isTable;
        }
    }
}