package Utils;

import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.util.List;
import java.util.ArrayList;
import java.io.OutputStream;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import java.io.File;
import org.apache.pdfbox.pdmodel.common.PDStream;

public class PdfHeaderFooterRemover {
    public static void main(String[] args) {
        String inputFile = "/Users/Jenius/Desktop/财评报告.pdf";
        String outputFile = "/Users/Jenius/Desktop/no_header_footer.pdf";

        try (PDDocument document = PDDocument.load(new File(inputFile))) {

            for (PDPage page : document.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                float width = mediaBox.getWidth();
                float height = mediaBox.getHeight();

                // 移除图像绘制操作
                PDFStreamParser parser = new PDFStreamParser((PDContentStream) page.getContents());
                parser.parse();
                List<Object> tokens = parser.getTokens();
                List<Object> newTokens = new ArrayList<>();

                for (int i = 0; i < tokens.size(); i++) {
                    Object token = tokens.get(i);
                    if (token instanceof Operator) {
                        Operator op = (Operator) token;
                        if ("Do".equals(op.getName())) {
                            if (i > 0 && tokens.get(i - 1) instanceof COSName) {
                                COSName objectName = (COSName) tokens.get(i - 1);
                                PDXObject xobject = page.getResources().getXObject(objectName);
                                if (xobject instanceof PDImageXObject) {
                                    // 跳过图像操作及其操作数
                                    continue;
                                }
                            }
                        }
                    }
                    newTokens.add(token);
                }

                PDStream updatedStream = new PDStream(document);
                try (OutputStream out = updatedStream.createOutputStream()) {
                    ContentStreamWriter writer = new ContentStreamWriter(out);
                    writer.writeTokens(newTokens);
                }
                page.setContents(updatedStream);

                // 覆盖页眉（顶部 50 像素）和页脚（底部 50 像素）
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {

                    contentStream.setNonStrokingColor(1.0f); // 设置白色

                    // 页眉区域
                    contentStream.addRect(0, 0, width, 125);
                    contentStream.fill();

                    // 页脚区域
                    contentStream.addRect(0, height-120, width, 120);
                    contentStream.fill();
                }
            }

            document.save(outputFile);
            System.out.println("处理完成，输出文件：" + outputFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}