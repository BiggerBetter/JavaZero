package Utils;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;

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
        // 我们简单约定：如果文本里出现换行 + “|”这样的结构，就认为后续是表格
        // (实际业务中可改成更严谨的判断方式)
        String[] lines = replaceValue.split("\n");
        if (lines.length == 1) {
            // 不含换行的简单文本
            XWPFRun run = paragraph.createRun();
            run.setText(replaceValue);
            return;
        }

        // 如果有多行，那我们尝试找第一行、后面的行中哪行开始是表头
        // 例如：第一行是 "销售额表："，第二行开始是表头 "| 产品 | 销量 |"
        // 这里可根据实际场景做更灵活的拆分
        StringBuilder textPart = new StringBuilder();
        List<String> tableLines = new ArrayList<>();

        boolean tableStarted = false;
        for (String line : lines) {
            String trimLine = line.trim();
            if (trimLine.startsWith("|")) {
                tableStarted = true;
            }
            if (!tableStarted) {
                // 还没开始表格部分，都当作文本
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
            // 如果占位符当前就在表格单元格中，则可以往同一个单元格中插入表格
            // 或者在文档后面追加表格，这里演示直接在所在段落后面追加一个段落 + 表格
            XWPFTable newTable = insertTableAfterParagraph(paragraph, doc, tableLines);
            // 后续可根据需要对 newTable 设置样式
        }
    }

    /**
     * 在指定段落后面插入一个表格(作为示例)。返回创建的表格对象。
     *
     * @param paragraph   当前段落
     * @param doc         文档对象
     * @param tableLines  markdown 表格的每一行(已trim)
     * @return 新创建的表格对象
     */
    private static XWPFTable insertTableAfterParagraph(XWPFParagraph paragraph,
                                                       XWPFDocument doc,
                                                       List<String> tableLines) {
        // 在当前段落之后新建一个段落
        // 注意：如果这个段落是在表格单元格里，可以改成 cell.addParagraph() 实现嵌套表格
        XWPFParagraph newPara = doc.insertNewParagraph(paragraph.getCTP().newCursor());

        // 创建表格
        XWPFTable table = doc.createTable();

        // 解析 markdown 样式的表格行
        // 第一行：表头，例如 "| 产品 | 销量 |"
        // 第二行：分割线，例如 "|----|----|"
        // 后续：数据行
        // (这里示例简单处理，并不严格校验所有可能情况)
        if (tableLines.size() < 2) {
            return table; // 数据不完整就直接返回
        }

        // 解析表头
        String headerLine = tableLines.get(0);
        // 去掉开头和结尾的 "|"
        headerLine = headerLine.replaceAll("^\\|", "").replaceAll("\\|$", "");
        String[] headers = headerLine.split("\\|");

        // 解析分隔线(此处可以不需要实际处理，只是跳过第二行)
        // String separateLine = tableLines.get(1);

        // 创建表头 XWPFTableRow
        XWPFTableRow headerRow = table.getRow(0); // POI 默认给了第一行
        // 先确保第一行有正确的列数
        for (int i = headerRow.getTableCells().size(); i < headers.length; i++) {
            headerRow.addNewTableCell();
        }

        for (int i = 0; i < headers.length; i++) {
            String headerText = headers[i].trim();
            headerRow.getCell(i).setText(headerText);
        }

        // 从第三行开始，是数据行
        for (int i = 2; i < tableLines.size(); i++) {
            String dataLine = tableLines.get(i);
            // 同样去掉头尾的 "|"
            dataLine = dataLine.replaceAll("^\\|", "").replaceAll("\\|$", "");
            String[] cols = dataLine.split("\\|");

            XWPFTableRow dataRow = table.createRow(); // 新增一行
            // 填充单元格
            for (int colIndex = 0; colIndex < cols.length; colIndex++) {
                String cellText = cols[colIndex].trim();
                dataRow.getCell(colIndex).setText(cellText);
            }
        }

        return table;
    }

}