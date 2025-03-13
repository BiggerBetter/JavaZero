package Utils;

import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class WordTemplateEngine {

    private final Map<String, String> variables;

    public WordTemplateEngine(Map<String, String> variables) {
        this.variables = variables;
    }

    /**
     * 处理整个Word文档
     */
    public void processDocument(String inputPath, String outputPath) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(inputPath))) {
            // 处理所有段落
            processParagraphs(doc.getParagraphs());

            // 处理所有表格
            for (XWPFTable table : doc.getTables()) {
                processTable(table);
            }

            // 保存结果
            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                doc.write(out);
            }
        }
    }

    /**
     * 处理段落集合
     */
    /**
     * 处理段落集合
     */
    private void processParagraphs(List<XWPFParagraph> paragraphs) {
        // 使用传统的 for 循环遍历段落
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);
            String originalText = getFullText(para);
            if (containsPlaceholder(originalText)) {
                if (isTableContent(originalText)) {
                    replaceParagraphWithTable(para);
                    i--; // 调整索引，因为段落被替换为表格
                } else {
                    replaceSimpleText(para);
                }
            }
        }
    }

    /**
     * 处理表格及其嵌套内容
     */
    private void processTable(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                // 递归处理单元格内容
                processParagraphs(cell.getParagraphs());
                for (XWPFTable nestedTable : cell.getTables()) {
                    processTable(nestedTable);
                }
            }
        }
    }

    /**
     * 替换普通文本
     */
    private void replaceSimpleText(XWPFParagraph para) {
        String mergedText = mergeRuns(para);
        String replacedText = replaceVariables(mergedText);

        // 清除原有内容
        for (int i = para.getRuns().size() - 1; i >= 0; i--) {
            para.removeRun(i);
        }

        // 添加新内容并保留格式
        if (!para.getRuns().isEmpty()) {
            XWPFRun firstRun = para.getRuns().get(0);
            XWPFRun newRun = para.createRun();
            newRun.setText(replacedText);
            copyRunStyle(firstRun, newRun);
        } else {
            para.createRun().setText(replacedText);
        }
    }

    /**
     * 将段落替换为表格
     */
    private void replaceParagraphWithTable(XWPFParagraph para) {
        String markdown = getMarkdownContent(mergeRuns(para));
        List<List<String>> tableData = parseMarkdownTable(markdown);

        XWPFDocument doc = para.getDocument();
        int paraPosition = doc.getPosOfParagraph(para);

        // 创建新表格
        XWPFTable table = doc.createTable();
        for (List<String> rowData : tableData) {
            if (rowData == null || rowData.isEmpty()) {
                continue; // 跳过空行
            }

            XWPFTableRow row = table.createRow();
            for (int i = 0; i < rowData.size(); i++) {
                String cellText = rowData.get(i);
                if (cellText == null) {
                    cellText = ""; // 如果单元格内容为空，设置为空字符串
                }

                XWPFTableCell cell = row.getCell(i);
                if (cell == null) {
                    cell = row.addNewTableCell(); // 如果单元格不存在，创建新的单元格
                }

                cell.setText(cellText); // 设置单元格内容

                // 继承原段落格式
                if (!cell.getParagraphs().isEmpty()) {
                    copyParagraphStyle(para, cell.getParagraphs().get(0));
                }
            }
        }

        // 将表格插入到指定位置
        doc.insertTable(paraPosition, table);

        // 移除原段落
        doc.removeBodyElement(paraPosition + 1);
    }

    //--------------------- 工具方法 ---------------------

    /**
     * 合并段落中的多个Run
     */
    private String mergeRuns(XWPFParagraph para) {
        return para.getRuns().stream()
                .map(run -> run.getText(0))
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    /**
     * 变量替换核心逻辑
     */
    private String replaceVariables(String text) {
        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 解析Markdown格式表格
     */
    private List<List<String>> parseMarkdownTable(String markdown) {
        return Arrays.stream(markdown.split("\n"))
                .filter(line -> line.startsWith("|"))
                .map(line -> Arrays.stream(line.split("\\|"))
                        .skip(1)
                        .map(String::trim)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * 判断是否包含占位符
     */
    private boolean containsPlaceholder(String text) {
        return text.contains("${");
    }

    /**
     * 判断是否为表格内容
     */
    private boolean isTableContent(String text) {
        return replaceVariables(text).contains("|") && replaceVariables(text).contains("\n");
    }

    /**
     * 获取Markdown表格内容
     */
    private String getMarkdownContent(String text) {
        Matcher matcher = Pattern.compile("\\$\\{(.+?)\\}").matcher(text);
        return matcher.find() ? variables.getOrDefault(matcher.group(1), "") : "";
    }

    /**
     * 复制段落样式
     */
    private void copyParagraphStyle(XWPFParagraph source, XWPFParagraph target) {
        target.setAlignment(source.getAlignment());
        target.setIndentationLeft(source.getIndentationLeft());
        // 可根据需要添加更多样式复制逻辑
    }

    /**
     * 复制Run样式
     */
    private void copyRunStyle(XWPFRun source, XWPFRun target) {
        target.setFontFamily(source.getFontFamily());
        target.setFontSize(source.getFontSize());
        target.setColor(source.getColor());
        target.setBold(source.isBold());
        target.setItalic(source.isItalic());
        // 可根据需要添加更多样式属性
    }

    //--------------------- 使用示例 ---------------------

    public static void main(String[] args) {
        // 示例变量数据
        Map<String, String> variables = new HashMap<>();
        variables.put("name", "张三");
        variables.put("department", "技术部");
        variables.put("report_date", "2023-10-01");
        variables.put("sales_data",
                "| 产品 | 销售额 |\n |------|--------|\n| 手机 | ¥12000 |\n| 电脑 | ¥25000 |");

        try {
            WordTemplateEngine engine = new WordTemplateEngine(variables);
            engine.processDocument("/Users/Jenius/Desktop/格式测试模版.docx",
                    "/Users/Jenius/Desktop/output.docx");
            System.out.println("文档生成成功！");
        } catch (IOException e) {
            System.err.println("文档处理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 获取段落中的完整文本内容
     */
    private String getFullText(XWPFParagraph para) {
        return para.getRuns().stream()
                .map(run -> run.getText(0))
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }
}