package Utils;

import org.apache.poi.xwpf.usermodel.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordPlaceholderWithFormat {

    // 匹配 ${...} 的占位符
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public static void main(String[] args) throws IOException {
        // 原始 .docx 文件路径
        String inputPath = "/Users/Jenius/Desktop/格式测试模版.docx";
        // 输出 .docx 文件路径
        String outputPath = "/Users/Jenius/Desktop/formation.docx";

        // 1. 打开原文档
        XWPFDocument originalDoc;
        try (FileInputStream fis = new FileInputStream(inputPath)) {
            originalDoc = new XWPFDocument(fis);
        }

        // 2. 新建目标文档
        XWPFDocument newDoc = new XWPFDocument();

        // 3. 遍历原文档的段落
        for (XWPFParagraph originalPara : originalDoc.getParagraphs()) {
            // 在新文档中创建对应的段落
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
            // newPara.setStyle(originalPara.getStyle());



            // 4. 遍历当前段落中的每个 run（它相当于一段连续的文本格式）
            for (XWPFRun originalRun : originalPara.getRuns()) {
                // 获取 run 的全部文字
                String runText = originalRun.toString();
                if (runText == null || runText.isEmpty()) {
                    // 如果内容为空，可以直接复制一个空 run 用来保持格式，也可跳过
                    XWPFRun r = newPara.createRun();
                    copyRunStyle(originalRun, r);
                    continue;
                }

                // 使用正则找占位符
                Matcher matcher = PLACEHOLDER_PATTERN.matcher(runText);

                int lastEnd = 0;
                // 每遇到一个占位符，就拆分这段 run 的文字
                while (matcher.find()) {
                    // 1) 拿到占位符前面的文字（不含占位符）
                    String textBefore = runText.substring(lastEnd, matcher.start());
                    if (!textBefore.isEmpty()) {
                        XWPFRun rBefore = newPara.createRun();
                        copyRunStyle(originalRun, rBefore);
                        rBefore.setText(textBefore);
                    }

                    // 2) 拿到占位符本身
                    String placeholder = matcher.group(0);     // 例如 "${...}"
                    String placeholderContent = matcher.group(1); // 例如 "..."
                    // 这里可以做替换逻辑
                    String replacedValue = processPlaceholder(placeholderContent);

                    // 把替换后的文本写到新 run 中
                    XWPFRun rPlaceholder = newPara.createRun();
                    copyRunStyle(originalRun, rPlaceholder);
                    rPlaceholder.setText(replacedValue);

                    lastEnd = matcher.end();
                }

                // 3) 占位符之后的剩余文字
                if (lastEnd < runText.length()) {
                    String textAfter = runText.substring(lastEnd);
                    if (!textAfter.isEmpty()) {
                        XWPFRun rAfter = newPara.createRun();
                        copyRunStyle(originalRun, rAfter);
                        rAfter.setText(textAfter);
                    }
                }
            }
        }

        // 5. 写出到结果文件
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            newDoc.write(fos);
        }

        System.out.println("处理完成，生成新文档：" + outputPath);
    }

    /**
     * 示例：根据占位符内部内容来确定要替换的文本
     * 可以根据实际需求改成从数据库或配置中取值
     */
    private static String processPlaceholder(String placeholderContent) {
        // 简单示例：placeholderContent + "_已替换"
        return placeholderContent + "_已替换";
    }

    /**
     * 将原 run 的常见样式复制到新的 run。
     * 可以根据实际需求添加/删除想要复制的属性。
     */
    private static void copyRunStyle(XWPFRun source, XWPFRun target) {
        // 字体加粗
        target.setBold(source.isBold());
        // 斜体
        target.setItalic(source.isItalic());
        // 下划线
        target.setUnderline(source.getUnderline());
        // 字体大小
        if (source.getFontSize() != -1) {
            target.setFontSize(source.getFontSize());
        }
        // 字体家族
        if (source.getFontFamily() != null) {
            target.setFontFamily(source.getFontFamily());
        }
        // 字体颜色
        if (source.getColor() != null) {
            target.setColor(source.getColor());
        }
        // 删除线
        target.setStrikeThrough(source.isStrikeThrough());
        // 文字突出显示颜色 (highlight)
        if (source.getTextHightlightColor() != null) {
            target.setTextHighlightColor(String.valueOf(source.getTextHightlightColor()));
        }
        // 也可以复制其它属性，例如阴影、排版位置等
        // ...
    }
}