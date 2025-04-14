package Utils;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

public class PdfPageExtractor {

    /**
     * 从PDF中提取指定页码范围的页面，生成新的PDF文件
     *
     * @param sourcePath 源PDF路径
     * @param targetPath 生成的新PDF路径
     * @param startPage  起始页（从1开始）
     * @param endPage    结束页（包含）
     */
    public static void extractPages(String sourcePath, String targetPath, int startPage, int endPage) {
        try (PDDocument sourceDoc = PDDocument.load(new File(sourcePath));
             PDDocument newDoc = new PDDocument()) {

            int totalPages = sourceDoc.getNumberOfPages();
            startPage = Math.max(1, startPage); // 确保至少从第一页开始
            endPage = Math.min(endPage, totalPages); // 不超过最大页数

            for (int i = startPage - 1; i < endPage; i++) {
                newDoc.addPage(sourceDoc.getPage(i));
            }

            newDoc.save(targetPath);
            System.out.println("提取完成，保存至: " + targetPath);

        } catch (IOException e) {
            System.err.println("发生错误: " + e.getMessage());
        }
    }

    // 示例调用
    public static void main(String[] args) {
        String sourcePdf = "/Users/Jenius/Desktop/财评报告.pdf";  // 原始PDF路径
        String targetPdf = "/Users/Jenius/Desktop/extracted_pages.pdf";  // 截取后的新PDF路径
        int start = 3; // 起始页
        int end = 10;  // 结束页

        extractPages(sourcePdf, targetPdf, start, end);
    }
}