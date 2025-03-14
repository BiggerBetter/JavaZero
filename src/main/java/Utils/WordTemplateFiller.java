package Utils;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
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
        for (XWPFRun run : new ArrayList<>(paragraph.getRuns())) {
            try {
                String text = run.getText(0);
                if (text != null) {
                    paragraphText.append(text);
                }
            } catch (org.apache.xmlbeans.impl.values.XmlValueDisconnectedException e) {
                // 如果该 run 已经与底层 XML 断开，则跳过
            }
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
     * 在指定段落“后面”插入一个新的段落 + 表格，而不是跑到文档末尾
     */
    private static XWPFTable insertTableAfterParagraph(XWPFParagraph paragraph,
                                                       XWPFDocument doc,
                                                       List<String> tableLines) {
        // 1) 找出 paragraph 在 doc.getParagraphs() 的位置
        int paragraphPos = doc.getParagraphs().indexOf(paragraph);
        if (paragraphPos < 0) {
            // 理论上不太会发生，除非这个段落不在当前 doc 里
            // 那就退回默认做法
            return createTableAtDocEnd(doc, tableLines);
        }

        // 2) 在 paragraphPos+1 的位置创建新段落
        //    这样它就会紧跟在原段落之后
        XWPFParagraph newPara = doc.createParagraph();
        // 将这个新的段落设到指定位置
        doc.setParagraph(newPara, paragraphPos + 1);

        // 3) 再创建一个表格并插入到“新段落”之后
        // POI 没有像“insertNewTable(索引)”之类的简单方法，需要在 XML 层操作；
        // 可以用“先创建，再移动”，或者干脆先在文档末尾创建，之后再删除临时位置。
        // 这里演示一个简化方案：直接在文档末尾创建表格，但马上移动到指定位置。

        XWPFTable tempTable = doc.createTable();
        // 填充数据
        fillMarkdownTable(tempTable, tableLines);

        // 4) 我们把刚才创建的表格“移”到 newPara 后面
        moveTableToPosition(doc, tempTable, paragraphPos + 2);

        return tempTable;
    }

    /** 备用：直接在文档末尾创建并填充表格 */
    private static XWPFTable createTableAtDocEnd(XWPFDocument doc, List<String> tableLines) {
        XWPFTable tbl = doc.createTable();
        fillMarkdownTable(tbl, tableLines);
        return tbl;
    }

    private static void moveTableToPosition(XWPFDocument doc, XWPFTable table, int newPos) {
        // Find the current position of the table in the document body
        List<IBodyElement> bodyElements = doc.getBodyElements();
        int oldPos = bodyElements.indexOf(table);
        if (oldPos == -1 || newPos < 0) {
            return; // Not found or invalid position
        }
        if (newPos == oldPos) {
            return; // Already in the desired position
        }

        // Clone the table's underlying CTTbl before removal to avoid orphaned XML issues
        CTTbl clonedCTTbl = (CTTbl) table.getCTTbl().copy();

        // Remove the table from its old position
        doc.removeBodyElement(oldPos);

        // Adjust newPos if the removal shifted the indices
        if (oldPos < newPos) {
            newPos--;
        }
        int currentSize = doc.getBodyElements().size();
        if (newPos > currentSize) {
            newPos = currentSize;
        }

        // Get the CTBody of the document
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody ctBody = doc.getDocument().getBody();

        // Create a new table at the end of the body
        CTTbl newCTTbl = ctBody.addNewTbl();

        // Set the new table's content using the cloned CT table
        newCTTbl.set(clonedCTTbl);

        // Get the DOM nodes for the body and the new table
        org.w3c.dom.Node bodyNode = ctBody.getDomNode();
        org.w3c.dom.Node newTblNode = newCTTbl.getDomNode();

        // Remove the newly added table node from its current position
        bodyNode.removeChild(newTblNode);

        // Insert the table node at the desired index
        org.w3c.dom.Node refNode = bodyNode.getChildNodes().item(newPos);
        if (refNode != null) {
            bodyNode.insertBefore(newTblNode, refNode);
        } else {
            bodyNode.appendChild(newTblNode);
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