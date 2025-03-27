package Utils;

import java.util.HashMap;
import java.util.Map;

public class EncodeCode {
    public static void main(String[] args) {
        String input = " public static List<List<String>> groupLines(String input) {\n" +
                "        List<List<String>> result = new ArrayList<>();\n" +
                "        String[] lines = input.split(\"\\\\n\");\n" +
                "\n" +
                "        List<String> currentGroup = null;\n" +
                "        // true 表示当前分组为表格行组；false 表示普通行组；初始值为 null\n" +
                "        Boolean currentType = null;\n" +
                "\n" +
                "        for (String line : lines) {\n" +
                "            String trimmed = line.trim();\n" +
                "            if (trimmed.isEmpty()) {\n" +
                "                // 如果空行，可以选择跳过\n" +
                "                continue;\n" +
                "            }\n" +
                "\n" +
                "            boolean isTableLine = trimmed.startsWith(\"|\") && trimmed.endsWith(\"|\");\n" +
                "\n" +
                "            // 如果当前还没有分组，或当前行与上一个分组类型不一致，则新开一个分组\n" +
                "            if (currentGroup == null || currentType == null || isTableLine != currentType) {\n" +
                "                currentGroup = new ArrayList<>();\n" +
                "                result.add(currentGroup);\n" +
                "                currentType = isTableLine;\n" +
                "            }\n" +
                "            currentGroup.add(trimmed);\n" +
                "        }\n" +
                "        return result;\n" +
                "    }";  // 输入文本
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
        map.put('a', "澳"); map.put('b', "波"); map.put('c', "兹"); map.put('d', "锝");
        map.put('e', "鹅"); map.put('f', "飞"); map.put('g', "高"); map.put('h', "蛤");
        map.put('i', "疑"); map.put('j', "机"); map.put('k', "可"); map.put('l', "叻");
        map.put('m', "马"); map.put('n', "那"); map.put('o', "呕"); map.put('p', "拍");
        map.put('q', "去"); map.put('r', "热"); map.put('s', "思"); map.put('t', "甜");
        map.put('u', "乌"); map.put('v', "唯"); map.put('w', "味"); map.put('x', "戏");
        map.put('y', "羊"); map.put('z', "籽");

        // 大写字母映射
        map.put('A', "嗄"); map.put('B', "班"); map.put('C', "草"); map.put('D', "东");
        map.put('E', "额"); map.put('F', "风"); map.put('G', "广"); map.put('H', "海");
        map.put('I', "翼"); map.put('J', "金"); map.put('K', "科"); map.put('L', "蓝");
        map.put('M', "明"); map.put('N', "南"); map.put('O', "欧"); map.put('P', "派");
        map.put('Q', "群"); map.put('R', "驲"); map.put('S', "膳"); map.put('T', "兲");
        map.put('U', "优"); map.put('V', "微"); map.put('W', "吻"); map.put('X', "西");
        map.put('Y', "阳"); map.put('Z', "志");

        // 常见符号映射
        map.put(' ', "空"); // 空格字符的映射

        return map;
    }
}
