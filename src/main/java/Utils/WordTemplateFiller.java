package Utils;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordTemplateFiller {

    // 匹配形如：${占位符} 的正则
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public static void main(String[] args) {
        // 准备替换数据
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", "张三");
        placeholders.put("department", "技术部");
        placeholders.put("report_date", "2023-10-01");
        // 示例：既包含表名，又包含 Markdown 风格表格
        placeholders.put("sales_data", "销售额表：\n| 产品 | 销量 |\n|----|----|\n| 手机 | 1000 |\n| 电脑 | 500 |");

        // 模版与结果输出路径可自定义
        String inputPath = "/Users/Jenius/Desktop/格式测试模版.docx";
        String outputPath = "/Users/Jenius/Desktop/outputByGPT-o1.docx";

        try (FileInputStream fis = new FileInputStream(inputPath);
             XWPFDocument doc = new XWPFDocument(fis)) {

            // 1. 先处理文档中不在表格里的段落
            List<XWPFParagraph> paragraphs = new ArrayList<>(doc.getParagraphs());
            for (XWPFParagraph paragraph : paragraphs) {
                processParagraph(paragraph, doc, placeholders);
            }

            // 2. 再递归处理文档中所有表格里的段落
            List<XWPFTable> tables = doc.getTables();
            for (XWPFTable table : tables) {
                processTable(table, doc, placeholders);
            }

            // 输出结果
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                doc.write(fos);
            }
            System.out.println("文档处理完成：" + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理普通段落，查找并替换其中的占位符。
     * 若替换内容包含表格，需额外插入表格。
     */
    private static void processParagraph(XWPFParagraph paragraph,
                                         XWPFDocument doc,
                                         Map<String, String> placeholders) {
        // 收集此段落中所有 run 的文本并合并
        StringBuilder paragraphText = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            paragraphText.append(run.toString());
        }

        // 找占位符
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(paragraphText.toString());
        if (!matcher.find()) {
            return; // 无占位符则不处理
        }

        // 如果找到占位符，则先清空原来的 runs，后面重建
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // 我们需要重新对段落文本进行逐个替换操作
        // 因为可能有多个占位符
        int lastIndex = 0;
        matcher.reset();
        while (matcher.find()) {
            // 占位符开始之前的纯文本
            String textBefore = paragraphText.substring(lastIndex, matcher.start());
            if (!textBefore.isEmpty()) {
                XWPFRun run = paragraph.createRun();
                run.setText(textBefore);
            }

            // 占位符内容
            String placeHolderKey = matcher.group(1); // group(1)匹配的是占位符内部的文字
            String replaceValue = placeholders.getOrDefault(placeHolderKey, "");

            // 写入占位符对应替换内容
            insertReplacement(paragraph, doc, replaceValue);

            lastIndex = matcher.end();
        }

        // 占位符之后的纯文本
        String textAfter = paragraphText.substring(lastIndex);
        if (!textAfter.isEmpty()) {
            XWPFRun run = paragraph.createRun();
            run.setText(textAfter);
        }
    }

    /**
     * 遍历并处理表格中的所有段落（表格->行->单元格->段落）。
     */
    private static void processTable(XWPFTable table,
                                     XWPFDocument doc,
                                     Map<String, String> placeholders) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                // 递归处理这个单元格里的段落
                List<XWPFParagraph> cellParagraphs = new ArrayList<>(cell.getParagraphs());
                for (XWPFParagraph p : cellParagraphs) {
                    processParagraph(p, doc, placeholders);
                }
                // 如果单元格里还嵌套表格（理论上也可能），可进一步递归
                for (XWPFTable nestedTable : cell.getTables()) {
                    processTable(nestedTable, doc, placeholders);
                }
            }
        }
    }

    /**
     * 根据替换内容，插入到当前段落中。
     * 若包含 Markdown 风格表格，则先写入前面的文本(若有)，再新建段落或表格。
     */
    private static void insertReplacement(XWPFParagraph paragraph,
                                          XWPFDocument doc,
                                          String replaceValue) {
        // 判断是否包含可能的 markdown 表格(竖线 + 换行)
        // 例如："销售额表：\n| 产品 | 销量 |\n|----|----|\n| 手机 | 1000 |\n| 电脑 | 500 |"
        // 这里简单约定：如果文本里出现换行 + “|”这样的结构，就认为后续是表格
        String[] lines = replaceValue.split("\n");
        if (lines.length == 1) {
            // 不含换行的简单文本
            XWPFRun run = paragraph.createRun();
            run.setText(replaceValue);
            return;
        }

        // 如果有多行，那我们尝试找出普通文本部分和表格部分
        StringBuilder textPart = new StringBuilder();
        List<String> tableLines = new ArrayList<>();

        boolean tableStarted = false;
        for (String line : lines) {
            String trimLine = line.trim();
            if (trimLine.startsWith("|")) {
                tableStarted = true;
            }
            if (!tableStarted) {
                // 还没开始表格部分，都当作普通文本
                textPart.append(line).append("\n");
            } else {
                // 属于表格部分
                tableLines.add(trimLine);
            }
        }

        // 写入文本部分（若有）
        if (textPart.length() > 0) {
            XWPFRun run = paragraph.createRun();
            // 去掉多余换行或可以只保留一个换行符
            run.setText(textPart.toString().trim());
        }

        // 如果检测到表格行，则插入一个新表格
        if (!tableLines.isEmpty()) {
            // 在所在段落后面插入一个段落 + 表格
            XWPFTable newTable = insertTableAfterParagraph(paragraph, doc, tableLines);
            // 根据需要，可对 newTable 设置样式
        }
    }

    /**
     * 在指定段落后面插入一个表格(作为示例)，如果段落在表格单元格里，
     * 则将表格插入到同一个单元格里；否则插到文档层。
     */
    private static XWPFTable insertTableAfterParagraph(XWPFParagraph paragraph,
                                                       XWPFDocument doc,
                                                       List<String> tableLines) {
        if (paragraph.getBody() instanceof XWPFTableCell) {
            // 如果当前段落位于表格单元格中：
            XWPFTableCell cell = (XWPFTableCell) paragraph.getBody();

            // （兼容做法）先在文档层创建一个临时表格
            XWPFTable tempTable = doc.createTable();
            // 填充 Markdown
            fillMarkdownTable(tempTable, tableLines);

            // 将临时表格的 XML 拷贝到单元格
            cell.getCTTc().addNewTbl().set(tempTable.getCTTbl());

            // 移除文档顶层生成的临时表格
            int posInDoc = doc.getBodyElements().indexOf(tempTable);
            if (posInDoc >= 0) {
                doc.removeBodyElement(posInDoc);
            }

            return tempTable;
        } else {
            // 如果不在表格单元格中，就在文档层插入
            // 先新建一个段落(用于分隔表格和之前的文字)
            XWPFParagraph newPara = doc.insertNewParagraph(paragraph.getCTP().newCursor());
            // 然后创建表格并填充
            XWPFTable newTable = doc.createTable();
            fillMarkdownTable(newTable, tableLines);
            return newTable;
        }
    }

    /**
     * 将 tableLines(已解析好的 Markdown 行) 填充到给定表格里。
     */
    private static void fillMarkdownTable(XWPFTable table, List<String> tableLines) {
        // 1. 判断是否有足够的行数（至少2行：第一行表头，第二行分隔线）
        if (tableLines.size() < 2) {
            return;
        }

        // 2. 解析表头（第1行）
        String headerLine = tableLines.get(0);
        headerLine = headerLine.replaceAll("^\\|", "").replaceAll("\\|$", "");
        String[] headers = headerLine.split("\\|");

        // 默认表格已自动创建一行
        XWPFTableRow headerRow = table.getRow(0);
        // 确保第一行有足够的单元格
        for (int i = headerRow.getTableCells().size(); i < headers.length; i++) {
            headerRow.addNewTableCell();
        }
        // 填充表头文本
        for (int i = 0; i < headers.length; i++) {
            headerRow.getCell(i).setText(headers[i].trim());
        }

        // 3. 从第三行(索引2)开始，是数据行
        for (int i = 2; i < tableLines.size(); i++) {
            String dataLine = tableLines.get(i);
            // 去掉开头和结尾的 "|"
            dataLine = dataLine.replaceAll("^\\|", "").replaceAll("\\|$", "");
            String[] cols = dataLine.split("\\|");

            XWPFTableRow dataRow = table.createRow(); // 新增一行
            // 逐列填充
            for (int colIndex = 0; colIndex < cols.length; colIndex++) {
                dataRow.getCell(colIndex).setText(cols[colIndex].trim());
            }
        }
    }
}