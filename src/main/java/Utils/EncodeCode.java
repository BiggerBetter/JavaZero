package Utils;

import java.util.HashMap;
import java.util.Map;

public class EncodeCode {
    public static void main(String[] args) {
        String input = "import java.util.regex.*;\n" +
                "import java.util.*;\n" +
                "\n" +
                "public class HtmlTableParser {\n" +
                "    public static void main(String[] args) {\n" +
                "        String html = \"< table > < tr > < td >A< /td > <td>B< /td>< /tr > <tr><td>1</td><td></td></tr> < /table >\";\n" +
                "\n" +
                "        // 去除HTML标签中的多余空格\n" +
                "        html = html.replaceAll(\"<\\\\s*\", \"<\").replaceAll(\"\\\\s*>\", \">\");\n" +
                "\n" +
                "        // 匹配表格行\n" +
                "        Pattern rowPattern = Pattern.compile(\"<tr>(.*?)</tr>\", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);\n" +
                "        Pattern cellPattern = Pattern.compile(\"<td>(.*?)</td>\", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);\n" +
                "\n" +
                "        Matcher rowMatcher = rowPattern.matcher(html);\n" +
                "        StringBuilder output = new StringBuilder();\n" +
                "\n" +
                "        while (rowMatcher.find()) {\n" +
                "            String rowContent = rowMatcher.group(1);\n" +
                "            Matcher cellMatcher = cellPattern.matcher(rowContent);\n" +
                "            List<String> cells = new ArrayList<>();\n" +
                "\n" +
                "            while (cellMatcher.find()) {\n" +
                "                // 去除单元格内的HTML标签与多余空格\n" +
                "                String cell = cellMatcher.group(1).replaceAll(\"<.*?>\", \"\").trim();\n" +
                "                cells.add(cell);\n" +
                "            }\n" +
                "\n" +
                "            // 拼接该行\n" +
                "            output.append(\"| \");\n" +
                "            output.append(String.join(\" | \", cells));\n" +
                "            output.append(\" |\\n\");\n" +
                "        }\n" +
                "\n" +
                "        System.out.println(output.toString());\n" +
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
        map.put('a', "澳"); map.put('b', "波"); map.put('c', "兹"); map.put('d', "锝");
        map.put('e', "鹅"); map.put('f', "飞"); map.put('g', "高"); map.put('h', "蛤");
        map.put('i', "疑"); map.put('j', "机"); map.put('k', "轲"); map.put('l', "叻");
        map.put('m', "马"); map.put('n', "那"); map.put('o', "呕"); map.put('p', "拍");
        map.put('q', "娶"); map.put('r', "热"); map.put('s', "思"); map.put('t', "甜");
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
        map.put(' ', "箜"); // 空格字符的映射

        return map;
    }
}
