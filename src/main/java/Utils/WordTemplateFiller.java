package Utils;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordTemplateFiller {

    // 匹配形如：${占位符} 的正则表达式
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public static void main(String[] args) {
        // 准备替换数据
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", "张三");
        placeholders.put("department", "技术部");
        placeholders.put("report_date", "2023-10-01");
        // 示例：既包含表头文本，又包含 Markdown 风格表格
        placeholders.put("sales_data", "销售额表：" +
                "\n| 产品 | 销量 |" +
                "\n|----|----|\n| 手机 | 1001 |\n| 电脑 | 501 |\n " +
                "我有一个梦想" +
                // "\n| 产品 | 销量 |\n|----|----|\n| 手机 | 1002 |\n| 电脑 | 502 |\n" +
                "有梦想就会有奇迹");

        // 模板文件和输出文件路径（可根据需要修改）
        String inputPath = "/Users/Jenius/Desktop/格式测试模版.docx";
        String outputPath = "/Users/Jenius/Desktop/outputByGPT-o1.docx";

        try (FileInputStream fis = new FileInputStream(inputPath);
             XWPFDocument doc = new XWPFDocument(fis)) {

            Path outputPathPath = Paths.get(outputPath);
            if (Files.exists(outputPathPath)) {
                Files.delete(outputPathPath);
            }

            // 1. 处理文档中非表格内的段落
            List<XWPFParagraph> paragraphs = new ArrayList<>(doc.getParagraphs());
            for (XWPFParagraph paragraph : paragraphs) {
                processParagraph(paragraph, doc, placeholders);
            }

            // 2. 递归处理文档中所有表格内的段落
            List<XWPFTable> tables = doc.getTables();
            for (XWPFTable table : tables) {
                processTable(table, doc, placeholders);
            }

            // 输出处理后的文档
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
     * 若替换内容包含表格，则额外插入表格。
     */
    private static void processParagraph(XWPFParagraph paragraph,
                                         XWPFDocument doc,
                                         Map<String, String> placeholders) {
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
            return; // 如果没有占位符则不处理
        }

        // 找到占位符后，清空原有的 runs，后续重建
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // 对段落文本进行逐个替换（可能存在多个占位符）
        int lastIndex = 0;
        matcher.reset();
        while (matcher.find()) {
            // 占位符之前的纯文本
            String textBefore = paragraphText.substring(lastIndex, matcher.start());
            if (!textBefore.isEmpty()) {
                XWPFRun run = paragraph.createRun();
                run.setText(textBefore);
            }

            // 获取占位符内部的内容
            String placeHolderKey = matcher.group(1);
            String replaceValue = placeholders.getOrDefault(placeHolderKey, "");//获取替换信息

            // 将占位符替换为对应内容（可能包含表格）
            insertReplacement(paragraph, doc, replaceValue);// 要调整的就是这个地方

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
     * 处理表格内的所有段落（遍历表格 -> 行 -> 单元格 -> 段落）。
     */
    private static void processTable(XWPFTable table, XWPFDocument doc, Map<String, String> placeholders) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                // 递归处理单元格内的段落
                List<XWPFParagraph> cellParagraphs = new ArrayList<>(cell.getParagraphs());
                for (XWPFParagraph p : cellParagraphs) {
                    processParagraph(p, doc, placeholders);
                }
                // 如果单元格中嵌套有表格，也进行递归处理
                // for (XWPFTable nestedTable : cell.getTables()) {
                //     processTable(nestedTable, doc, placeholders);
                // }
            }
        }
    }

    /**
     * 根据替换内容，将内容插入到当前段落中。
     * 如果内容包含 Markdown 风格的表格，则先写入文本部分，再插入新的段落和表格。
     */
    private static void insertReplacement(XWPFParagraph paragraph,
                                          XWPFDocument doc,
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

        int time = 1;
        XWPFParagraph cyclePara = paragraph;
        for (List<String> contentBlock : contentList) {
            String firstLine = contentBlock.get(0).trim();
            if (firstLine.startsWith("|") && firstLine.endsWith("|")) {
                XWPFTable newTable = insertTableAfterParagraph(cyclePara, doc, contentBlock);
                // 如有需要，可在此处对 newTable 设置其他样式
                XWPFParagraph tmpPara  = doc.createParagraph();//填入空段落
                cyclePara = tmpPara;
                tmpPara.createRun().setText("test after table"+time);
                time = time +1;
            } else {
                // 进入文本段落插入逻辑
                XWPFParagraph newPara = doc.createParagraph();
                XWPFRun run = newPara.createRun();
                run.setText(String.join("\n",contentBlock).trim());
                cyclePara = newPara;
            }
        }



        // todo 要兼容多表格多文本样式
        // StringBuilder textPart = new StringBuilder();
        // List<String> tableLines = new ArrayList<>();
        //
        // // boolean tableStarted = false;
        // for (String line : lines) {
        //     String trimLine = line.trim();
        //     if (trimLine.startsWith("|") && trimLine.endsWith("|") ) {
        //         // 作为表格部分处理
        //         tableLines.add(trimLine);
        //     }else {
        //         textPart.append(line).append("\n");
        //     }
        // }
        //
        // // 写入文本部分（如果存在）
        // if (textPart.length() > 0) {
        //     XWPFRun run = paragraph.createRun();
        //     run.setText(textPart.toString().trim());
        // }
        //
        // // 如果检测到表格行，则在当前段落后插入新的段落和表格
        // if (!tableLines.isEmpty()) {
        //     XWPFTable newTable = insertTableAfterParagraph(paragraph, doc, tableLines);
        //     // 如有需要，可在此处对 newTable 设置其他样式
        // }
    }

    /**
     * 在指定段落后插入一个新的段落和表格，而不是将表格直接插入到文档末尾。
     */
    private static XWPFTable insertTableAfterParagraph(XWPFParagraph paragraph,
                                                       XWPFDocument doc,
                                                       List<String> tableLines) {
        // 1) 获取当前段落在文档中所有段落中的索引
        int paragraphPos = doc.getParagraphs().indexOf(paragraph);

        // 2) 在该段落后创建一个新的段落，使其紧跟在原段落之后
        XWPFParagraph newPara = doc.createParagraph();
        doc.setParagraph(newPara, paragraphPos + 1);

        // 3) 在文档中创建一个表格，并填充数据
        XWPFTable tempTable = doc.createTable();
        fillMarkdownTable(tempTable, tableLines);

        // 4) 将创建的表格移动到新段落后（即原段落后第二个位置）
        moveTableToPosition(doc, tempTable, paragraphPos + 1);

        return tempTable;
    }

    /**
     * 将文档中已有的表格移动到指定的索引位置。
     * 此方法先克隆表格底层的 CTTbl，以避免 XML 脱离关联的问题，
     * 然后通过 DOM 操作将该表格插入到文档指定位置。
     */
    private static void moveTableToPosition(XWPFDocument doc, XWPFTable table, int newPos) {
        // 在文档的 bodyElements 中查找该表格的当前位置
        List<IBodyElement> bodyElements = doc.getBodyElements();
        int oldPos = bodyElements.indexOf(table);
        if (oldPos == -1 || newPos < 0) {
            return; // 未找到表格或索引无效
        }
        if (newPos == oldPos) {
            return; // 表格已在目标位置，无需移动
        }

        // 克隆表格的底层 CTTbl，避免出现 XML 脱离关联的问题
        CTTbl clonedCTTbl = (CTTbl) table.getCTTbl().copy();

        // 从文档中移除原有表格
        doc.removeBodyElement(oldPos);

        // 调整索引：如果旧位置小于新位置，则新位置减一
        if (oldPos < newPos) {
            newPos--;
        }
        int currentSize = doc.getBodyElements().size();
        if (newPos > currentSize) {
            newPos = currentSize;
        }

        // 获取文档的 CTBody 对象
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody ctBody = doc.getDocument().getBody();

        // 在文档末尾创建一个新的表格节点
        CTTbl newCTTbl = ctBody.addNewTbl();

        // 用克隆的 CT 表格内容设置新表格
        newCTTbl.set(clonedCTTbl);

        // 获取 CTBody 和新表格的 DOM 节点
        org.w3c.dom.Node bodyNode = ctBody.getDomNode();
        org.w3c.dom.Node newTblNode = newCTTbl.getDomNode();

        // 从当前节点中移除新表格节点
        bodyNode.removeChild(newTblNode);

        // 在指定索引位置插入该表格节点
        org.w3c.dom.Node refNode = bodyNode.getChildNodes().item(newPos);
        if (refNode != null) {
            bodyNode.insertBefore(newTblNode, refNode);
        } else {
            bodyNode.appendChild(newTblNode);
        }
    }

    /**
     * 将解析后的 Markdown 表格行填充到指定表格中，并设置表格及单元格内容居中。
     */
    private static void fillMarkdownTable(XWPFTable table, List<String> tableLines) {
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
}