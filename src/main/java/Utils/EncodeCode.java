package Utils;

import java.util.HashMap;
import java.util.Map;

public class EncodeCode {
    public static void main(String[] args) {
        String input = "import org.apache.pdfbox.pdmodel.PDDocument;\n" +
                "import org.apache.pdfbox.text.PDFTextStripper;\n" +
                "import java.util.regex.Pattern;\n" +
                "import java.util.List;\n" +
                "import java.util.ArrayList;\n" +
                "import java.io.File;\n" +
                "\n" +
                "public class PdfLineBreaksFixer {\n" +
                "    // 匹配章节标题的正则表达式（如：第一章、二、等）\n" +
                "    private static final Pattern CHAPTER_PATTERN = Pattern.compile(\n" +
                "        \"(第[一二三四五六七八九十]+章\\\\s*$)|(^[一二三四五六七八九十]+、)\"\n" +
                "    );\n" +
                "    // 匹配段落结束符号（。！？：）后跟结束符的情况\n" +
                "    private static final Pattern SECTION_END_PATTERN = Pattern.compile(\".*[。！？：]$\");\n" +
                "    // 匹配汉字或非分段字符\n" +
                "    private static final Pattern CONTINUOUS_CHAR_PATTERN = Pattern.compile(\"[\\u4e00-\\u9fa5,；]$\");\n" +
                "    public static void main(String[] args) throws Exception {\n" +
                "        String text = extractTextFromPdf(\"input.pdf\");\n" +
                "        String cleanedText = fixLineBreaks(text);\n" +
                "        System.out.println(cleanedText);\n" +
                "    }\n" +
                "    private static String extractTextFromPdf(String filePath) throws Exception {\n" +
                "        try (PDDocument document = PDDocument.load(new File(filePath))) {\n" +
                "            return new PDFTextStripper().getText(document);\n" +
                "        }\n" +
                "    }\n" +
                "    private static String fixLineBreaks(String rawText) {\n" +
                "        List<String> lines = new ArrayList<>(List.of(rawText.split(\"\\\\r?\\\\n\")));\n" +
                "        List<String> result = new ArrayList<>();\n" +
                "        int i = 0;\n" +
                "        while (i < lines.size()) {\n" +
                "            String currentLine = lines.get(i).trim();\n" +
                "            if (currentLine.isEmpty()) {\n" +
                "                i++;\n" +
                "                continue;\n" +
                "            }\n" +
                "            // 检查是否是章节标题\n" +
                "            if (isChapterTitle(currentLine)) {\n" +
                "                result.add(currentLine);\n" +
                "                i++;\n" +
                "                continue;\n" +
                "            }\n" +
                "            // 检查是否段落结束\n" +
                "            if (isSectionEnd(currentLine)) {\n" +
                "                result.add(currentLine);\n" +
                "                i++;\n" +
                "                continue;\n" +
                "            }\n" +
                "            // 尝试合并后续行\n" +
                "            StringBuilder mergedLine = new StringBuilder(currentLine);\n" +
                "            while (i < lines.size() - 1 && shouldMerge(currentLine, lines.get(i + 1))) {\n" +
                "                mergedLine.append(lines.get(i + 1).trim());\n" +
                "                i++;\n" +
                "                currentLine = mergedLine.toString();\n" +
                "            }\n" +
                "            result.add(mergedLine.toString());\n" +
                "            i++;\n" +
                "        }\n" +
                "        return String.join(\"\\n\", result);\n" +
                "    }\n" +
                "    private static boolean isChapterTitle(String line) {return CHAPTER_PATTERN.matcher(line).find();}\n" +
                "    private static boolean isSectionEnd(String line) {return SECTION_END_PATTERN.matcher(line).matches();}\n" +
                "\n" +
                "    private static boolean shouldMerge(String currentLine, String nextLine) {\n" +
                "        String cleanNext = nextLine.trim();\n" +
                "        if (cleanNext.isEmpty()) return false;\n" +
                "        // 检查当前行尾和下一行首的字符是否都是连续字符\n" +
                "        boolean currentEndsWithContinuous = CONTINUOUS_CHAR_PATTERN.matcher(currentLine).find();\n" +
                "        boolean nextStartsWithContinuous = CONTINUOUS_CHAR_PATTERN.matcher(cleanNext.substring(0, 1)).find();\n" +
                "        return currentEndsWithContinuous && nextStartsWithContinuous;\n" +
                "    }\n" +
                "}";  // 输入文本
        // System.out.println("原始文本: " + input);
        String encodedText = encode(input);
        System.out.println("编码后的文本: " + encodedText);
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
        map.put('I', "一"); map.put('J', "金"); map.put('K', "科"); map.put('L', "蓝");
        map.put('M', "明"); map.put('N', "南"); map.put('O', "欧"); map.put('P', "派");
        map.put('Q', "群"); map.put('R', "日"); map.put('S', "山"); map.put('T', "天");
        map.put('U', "优"); map.put('V', "微"); map.put('W', "文"); map.put('X', "西");
        map.put('Y', "阳"); map.put('Z', "志");

        // 常见符号映射
        map.put(',', "，"); map.put('!', "！"); map.put('.', "。"); map.put('?', "？");
        map.put(' ', "空"); // 空格字符的映射

        return map;
    }
}
