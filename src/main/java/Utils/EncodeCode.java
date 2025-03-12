package Utils;

import java.util.HashMap;
import java.util.Map;

public class EncodeCode {
    public static void main(String[] args) {
        String input = "import org.apache.pdfbox.pdmodel.PDDocument;\n" +
                "import org.apache.pdfbox.pdmodel.PDPage;\n" +
                "import org.apache.pdfbox.pdmodel.PDPageContentStream;\n" +
                "import org.apache.pdfbox.text.PDFTextStripper;\n" +
                "\n" +
                "import java.io.File;\n" +
                "import java.io.IOException;\n" +
                "import java.util.List;\n" +
                "\n" +
                "public class PDFMergeExample {\n" +
                "\n" +
                "    public static void main(String[] args) throws IOException {\n" +
                "        // 合并PDF文件\n" +
                "        PDDocument document1 = PDDocument.load(new File(\"document1.pdf\"));\n" +
                "        PDDocument document2 = PDDocument.load(new File(\"document2.pdf\"));\n" +
                "        \n" +
                "        // 合并\n" +
                "        document1.addPages(document2.getPages());\n" +
                "        \n" +
                "        // 保存合并后的文件\n" +
                "        document1.save(\"merged_document.pdf\");\n" +
                "        document1.close();\n" +
                "        document2.close();\n" +
                "        \n" +
                "        // 将TXT文件添加到PDF\n" +
                "        PDDocument txtDocument = new PDDocument();\n" +
                "        PDPage page = new PDPage();\n" +
                "        txtDocument.addPage(page);\n" +
                "        \n" +
                "        PDPageContentStream contentStream = new PDPageContentStream(txtDocument, page);\n" +
                "        contentStream.beginText();\n" +
                "        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);\n" +
                "        contentStream.newLineAtOffset(25, 750);\n" +
                "        \n" +
                "        // 读取TXT文件\n" +
                "        String text = new String(Files.readAllBytes(Paths.get(\"document.txt\")));\n" +
                "        contentStream.showText(text);\n" +
                "        \n" +
                "        contentStream.endText();\n" +
                "        contentStream.close();\n" +
                "        \n" +
                "        // 保存为PDF\n" +
                "        txtDocument.save(\"text_to_pdf.pdf\");\n" +
                "        txtDocument.close();\n" +
                "    }\n" +
                "}";  // 输入文本
        // System.out.println("原始文本: " + input);
        String encodedText = encode(input);
        System.out.println("编码后的文本: ");
        System.out.println(encodedText);
    }

    // 编码方法
    public static String encode(String input) {
        Map<Character, String> encodingMap = createEncodingMap();
        StringBuilder encodedText = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (encodingMap.containsKey(c)) {
                encodedText.append(encodingMap.get(c));
            } else {
                encodedText.append(c);  // 非映射字符保持不变
            }
        }

        return encodedText.toString();
    }

    // 创建编码映射表
    public static Map<Character, String> createEncodingMap() {
        Map<Character, String> map = new HashMap<>();

        // 小写字母映射
        map.put('a', "啊"); map.put('b', "波"); map.put('c', "次"); map.put('d', "的");
        map.put('e', "鹅"); map.put('f', "飞"); map.put('g', "高"); map.put('h', "哈");
        map.put('i', "疑"); map.put('j', "机"); map.put('k', "可"); map.put('l', "了");
        map.put('m', "马"); map.put('n', "那"); map.put('o', "呕"); map.put('p', "拍");
        map.put('q', "去"); map.put('r', "热"); map.put('s', "思"); map.put('t', "甜");
        map.put('u', "乌"); map.put('v', "唯"); map.put('w', "未"); map.put('x', "戏");
        map.put('y', "羊"); map.put('z', "子");

        // 大写字母映射
        map.put('A', "阿"); map.put('B', "班"); map.put('C', "草"); map.put('D', "东");
        map.put('E', "额"); map.put('F', "风"); map.put('G', "广"); map.put('H', "海");
        map.put('I', "已"); map.put('J', "金"); map.put('K', "科"); map.put('L', "蓝");
        map.put('M', "明"); map.put('N', "南"); map.put('O', "欧"); map.put('P', "派");
        map.put('Q', "群"); map.put('R', "日"); map.put('S', "山"); map.put('T', "天");
        map.put('U', "优"); map.put('V', "微"); map.put('W', "吻"); map.put('X', "西");
        map.put('Y', "阳"); map.put('Z', "志");

        // 常见符号映射
        map.put(' ', "空"); // 空格字符的映射

        return map;
    }
}
