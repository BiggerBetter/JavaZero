package Utils;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WordPlaceholderWithFormat{

    // 匹配 ${...} 的占位符
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final XWPFDocument newDoc = new XWPFDocument();

    private static final Map<String, String> placeholders = new HashMap<>();
    static {
        placeholders.put("name", "张三");
        placeholders.put("department", "技术部");
        placeholders.put("report_date", "2023-10-01");
        // 示例：既包含表头文本，又包含 Markdown 风格表格
        placeholders.put("sales_data", "销售额表：" +
                "\n| 产品 | 销量 |" +
                "\n|----|----|\n| 手机 | 1001 |\n| 电脑 | 501 |\n " +
                "我有一个梦想" +
                "\n| 产品 | 销量 |\n|----|----|\n| 手机 | 1002 |\n| 电脑 | 502 |\n" +
                "有梦想就会有奇迹");
    }

    public static void main(String[] args) throws IOException {
        // 原始 .docx 文件路径
        String inputPath = "/Users/Jenius/Desktop/格式测试模版.docx";
        // 输出 .docx 文件路径
        String outputPath = "/Users/Jenius/Desktop/formation-1.docx";

        Path outputPathPath = Paths.get(outputPath);
        if (Files.exists(outputPathPath)) {
            Files.delete(outputPathPath);
        }

        // 打开原文档
        XWPFDocument originalDoc;
        try (FileInputStream fis = new FileInputStream(inputPath)) {
            originalDoc = new XWPFDocument(fis);
        }

        for (IBodyElement element : originalDoc.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                // 填充段落
                processParagraph((XWPFParagraph)element);
            } else if (element instanceof XWPFTable) {
                // 填充表格
                processTable((XWPFTable)element);
            }
        }

        // 5. 写出到结果文件
        try  {
            FileOutputStream fos = new FileOutputStream(outputPath);
            newDoc.write(fos);
            System.out.println("处理完成，生成新文档：" + outputPath);
        }catch (Exception e){
            System.out.println("发生错误：" + e.getMessage());
        }
    }


    /**
     * 处理普通段落，查找并替换其中的占位符。
     * 若替换内容包含表格，则额外插入表格。
     */
    public static void processTextInTable(XWPFParagraph paragraph) {

        // 收集该段落中所有 run 的文本并合并
        StringBuilder paragraphText = new StringBuilder();
        for (XWPFRun run : new ArrayList<>(paragraph.getRuns())) {
            try {
                String text = run.getText(0);
                if (text != null) {
                    paragraphText.append(text);
                }
            } catch (Exception e) {
                // 如果该 run 与底层 XML 断开，则跳过
            }
        }

        // 查找占位符
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(paragraphText.toString());
        if (!matcher.find()) {
            return;
        }

        // 找到占位符后，清空原有的 runs，后续重建
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // 对段落文本进行逐个替换（可能存在多个占位符）
        int lastIndex = 0;
        matcher.reset();
        String textBefore = "";
        while (matcher.find()) {
            // 占位符之前的纯文本
            textBefore = paragraphText.substring(lastIndex, matcher.start());
            if (!textBefore.isEmpty()) {
                XWPFRun run = paragraph.createRun();
                run.setText(textBefore);
            }

            // 获取占位符内部的内容
            String placeHolderKey = matcher.group(1);
            String replaceValue = placeholders.getOrDefault(placeHolderKey, "");// todo 正式代码中删除

            // 将占位符替换为对应内容（可能包含表格）
            insertReplacement(paragraph, replaceValue);

            lastIndex = matcher.end();
        }

        // 占位符之后的纯文本  其实只要涉及但段落多次替换，就都不会插入新段落段落
        String textAfter = paragraphText.substring(lastIndex);
        if (!textAfter.isEmpty() && ! textBefore.isEmpty()) {
            XWPFRun run = paragraph.createRun();
            run.setText(textAfter);
        }
    }

    /**
     * 处理普通段落，查找并替换其中的占位符。
     * 若替换内容包含表格，则额外插入表格。
     */
    public static void processParagraph(XWPFParagraph originalPara) {
        // 在newDoc中新增段落，并保留原段落格式
        XWPFParagraph newPara = newDoc.createParagraph();

        // 可根据需求复制更多段落层级的属性，比如段落对齐方式：
        newPara.setAlignment(originalPara.getAlignment());
        newPara.setVerticalAlignment(originalPara.getVerticalAlignment());
        newPara.setBorderBetween(originalPara.getBorderBetween());
        newPara.setBorderBottom(originalPara.getBorderBottom());
        newPara.setBorderTop(originalPara.getBorderTop());
        newPara.setBorderLeft(originalPara.getBorderLeft());
        newPara.setBorderRight(originalPara.getBorderRight());
        // 保留首行缩进（首行锁进）
        newPara.setIndentationFirstLine(originalPara.getIndentationFirstLine());
        // 保留段落样式，如标题样式
        newPara.setStyle(originalPara.getStyle());
        // 如果有自定义样式名，也可以直接复制


        // 收集该段落中所有 run 的文本并合并
        StringBuilder paragraphText = new StringBuilder();
        for (XWPFRun run : new ArrayList<>(originalPara.getRuns())) {
            try {
                String text = run.getText(0);
                if (text != null) {
                    paragraphText.append(text);
                }
            } catch (Exception e) {
                // 如果该 run 与底层 XML 断开，则跳过
            }
        }

        // 查找占位符
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(paragraphText.toString());
        if (!matcher.find()) {
            // 如果没有占位符则拷贝旧段落到新doc
            copyParagraph(originalPara, newPara);
            return;
        }

        // 对段落文本进行逐个替换（可能存在多个占位符）
        int lastIndex = 0;
        matcher.reset();
        String textBefore = "";
        while (matcher.find()) {
            // 占位符之前的纯文本
            textBefore = paragraphText.substring(lastIndex, matcher.start());
            if (!textBefore.isEmpty()) {
                XWPFRun run = newPara.createRun();
                run.setText(textBefore);
            }

            // 获取占位符内部的内容
            String placeHolderKey = matcher.group(1);
            String replaceValue = placeholders.getOrDefault(placeHolderKey, "");// todo 正式代码中删除

            // 将占位符替换为对应内容（可能包含表格）
            insertReplacement(newPara, replaceValue);

            lastIndex = matcher.end();
        }

        // 占位符之后的纯文本  其实只要涉及但段落多次替换，就都不会插入新段落段落
        String textAfter = paragraphText.substring(lastIndex);
        if (!textAfter.isEmpty() && ! textBefore.isEmpty()) {
            XWPFRun run = newPara.createRun();
            run.setText(textAfter);
        }
    }

    /**
     * 根据替换内容，将内容插入到当前段落中。
     * 如果内容包含 Markdown 风格的表格，则先写入文本部分，再插入新的段落和表格。
     */
    public static void insertReplacement(XWPFParagraph paragraph,
                                         String replaceValue) {
        // 判断是否包含类似 Markdown 表格的格式（竖线和换行）
        String[] lines = replaceValue.split("\n");
        if (lines.length == 1) {
            // 如果仅为单行文本，则直接写入
            XWPFRun run = paragraph.createRun();
            run.setText(replaceValue);
            return;
        }

        List<List<String>> contentList = LineGrouper.groupLines(replaceValue);

        for (List<String> contentBlock : contentList) {
            String firstLine = contentBlock.get(0).trim();
            if (firstLine.startsWith("|") && firstLine.endsWith("|")) {
                XWPFTable newTable = createTable(contentBlock);
                // 如有需要，可在此处对 newTable 设置其他样式
            } else {
                // 进入文本段落插入逻辑
                XWPFParagraph newPara = newDoc.createParagraph();
                XWPFRun run = newPara.createRun();
                run.setText(String.join("\n",contentBlock).trim());
            }
        }
    }


    /**
     * 在指定段落后插入一个新的段落和表格，而不是将表格直接插入到文档末尾。
     */
    public static XWPFTable createTable(List<String> tableLines) {
        // 在文档中创建一个表格，并填充数据
        XWPFTable tempTable = newDoc.createTable();
        fillMarkdownTable(tempTable, tableLines);

        return tempTable;
    }

    /**
     * 将解析后的 Markdown 表格行填充到指定表格中，并设置表格及单元格内容居中。
     */
    public static void fillMarkdownTable(XWPFTable table, List<String> tableLines) {
        // 如果表格行数不足（至少需要两行：表头和分隔线），则直接返回
        if (tableLines.size() < 2) {
            return;
        }

        // 设置表格自动调整布局（自动宽度）
        if (table.getCTTbl().getTblPr() == null) {
            table.getCTTbl().addNewTblPr();
        }
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        // 设置自动布局（自动调整列宽）
        if (!tblPr.isSetTblLayout()) {
            tblPr.addNewTblLayout().setType(STTblLayoutType.AUTOFIT);
        } else {
            tblPr.getTblLayout().setType(STTblLayoutType.AUTOFIT);
        }
        // 设置表格宽度为自动
        if (!tblPr.isSetTblW()) {
            tblPr.addNewTblW().setType(STTblWidth.AUTO);
            tblPr.getTblW().setW(BigInteger.ZERO);
        } else {
            tblPr.getTblW().setType(STTblWidth.AUTO);
            tblPr.getTblW().setW(BigInteger.ZERO);
        }
        // 设置表格整体居中
        table.setTableAlignment(TableRowAlign.CENTER);

        // 解析表头（第一行）
        String headerLine = tableLines.get(0);
        headerLine = headerLine.replaceAll("^\\|", "").replaceAll("\\|$", "");
        String[] headers = headerLine.split("\\|");

        // 默认表格已创建第一行
        XWPFTableRow headerRow = table.getRow(0);
        // 确保表头行中单元格数量足够
        for (int i = headerRow.getTableCells().size(); i < headers.length; i++) {
            headerRow.addNewTableCell();
        }
        // 填充表头文本，并设置单元格内容居中
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            cell.setText(headers[i].trim());
            // 设置单元格垂直居中
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            // 将单元格内所有段落设置水平居中
            for (XWPFParagraph para : cell.getParagraphs()) {
                para.setAlignment(ParagraphAlignment.CENTER);
            }
        }

        // 从第三行开始填充数据行（即索引为 2 及以后的行）
        for (int i = 2; i < tableLines.size(); i++) {
            String dataLine = tableLines.get(i);
            // 去除行首和行尾的竖线
            dataLine = dataLine.replaceAll("^\\|", "").replaceAll("\\|$", "");
            String[] cols = dataLine.split("\\|");

            XWPFTableRow dataRow = table.createRow(); // 新建数据行
            // 遍历每一列，并设置文本和居中格式
            for (int colIndex = 0; colIndex < cols.length; colIndex++) {
                XWPFTableCell cell;
                // 如果当前数据行已有对应的单元格则使用，否则新增
                if (colIndex < dataRow.getTableCells().size()) {
                    cell = dataRow.getCell(colIndex);
                } else {
                    cell = dataRow.addNewTableCell();
                }
                cell.setText(cols[colIndex].trim());
                // 设置单元格垂直居中
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                // 设置单元格内所有段落水平居中
                for (XWPFParagraph para : cell.getParagraphs()) {
                    para.setAlignment(ParagraphAlignment.CENTER);
                }
            }
        }

        // 设置所有行的高度为自动（Word 会根据内容自动调整）
        for (XWPFTableRow row : table.getRows()) {
            row.setHeightRule(TableRowHeightRule.AUTO);
        }
    }

    /**
     * 处理表格内的所有段落（遍历表格 -> 行 -> 单元格 -> 段落）。
     */
    public static void processTable(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                // 递归处理单元格内的段落
                List<XWPFParagraph> cellParagraphs = new ArrayList<>(cell.getParagraphs());
                for (XWPFParagraph p : cellParagraphs) {
                    processTextInTable(p);
                }
            }
        }
        // 把填充后的表格，存储到新doc中
        XWPFTable newTable = newDoc.createTable();
        copyTable(table,newTable);
    }


    // 复制段落内容
    private static void copyParagraph(XWPFParagraph oldPara, XWPFParagraph newPara) {
        newPara.setAlignment(oldPara.getAlignment());
        newPara.setSpacingBefore(oldPara.getSpacingBefore());
        newPara.setSpacingAfter(oldPara.getSpacingAfter());
        newPara.setIndentationLeft(oldPara.getIndentationLeft());
        newPara.setIndentationRight(oldPara.getIndentationRight());

        for (XWPFRun run : oldPara.getRuns()) {
            XWPFRun newRun = newPara.createRun();
            newRun.setText(run.text());
            newRun.setBold(run.isBold());
            newRun.setItalic(run.isItalic());
            newRun.setUnderline(run.getUnderline());
            newRun.setFontSize(run.getFontSize());
            newRun.setFontFamily(run.getFontFamily());
            newRun.setColor(run.getColor());
        }
    }

    // 复制表格内容
    private static void copyTable(XWPFTable oldTable, XWPFTable newTable) {
        newTable.getCTTbl().setTblPr(oldTable.getCTTbl().getTblPr()); // 复制表格属性

        for (int i = 0; i < oldTable.getRows().size(); i++) {
            XWPFTableRow oldRow = oldTable.getRow(i);
            XWPFTableRow newRow = newTable.createRow();

            for (int j = 0; j < oldRow.getTableCells().size(); j++) {
                XWPFTableCell oldCell = oldRow.getCell(j);
                XWPFTableCell newCell = newRow.getCell(j);

                if (newCell == null) {
                    newCell = newRow.createCell();
                }

                newCell.getCTTc().setTcPr(oldCell.getCTTc().getTcPr()); // 复制单元格属性
                newCell.setText(oldCell.getText());
            }
        }
    }

}