package Utils;
import java.util.ArrayList;
import java.util.List;

public class LineGrouper {
    /**
     * 按行分组：
     * 若当前行以'|'开头且以'|'结尾，则视为表格行；
     * 否则视为普通行。
     * 遇到不同类型的行时，新建一个分组。
     */
    public static List<List<String>> groupLines(String input) {
        List<List<String>> result = new ArrayList<>();
        String[] lines = input.split("\\n");

        List<String> currentGroup = null;
        // true 表示当前分组为表格行组；false 表示普通行组；初始值为 null
        Boolean currentType = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                // 如果空行，可以选择跳过
                continue;
            }

            boolean isTableLine = trimmed.startsWith("|") && trimmed.endsWith("|");

            // 如果当前还没有分组，或当前行与上一个分组类型不一致，则新开一个分组
            if (currentGroup == null || currentType == null || isTableLine != currentType) {
                currentGroup = new ArrayList<>();
                result.add(currentGroup);
                currentType = isTableLine;
            }
            currentGroup.add(trimmed);
        }
        return result;
    }

    public static void main(String[] args) {
        String input = "销售额表：\n"
                + "| 产品 | 销量 |\n"
                + "|----|----|\n"
                + "| 手机 | 1000 |\n"
                + "| 电脑 | 500 |\n"
                + "价格表信息为： \n"
                + "哇哈哈哈： \n"
                + "阿斯顿发： \n"
                + "| 产品 | 价格 |\n"
                + "|----|----|\n"
                + "| 手机 | 998 |\n"
                + "| 电脑 | 3498 | \n"
                + "|总体收入为100w。";

        List<List<String>> groups = groupLines(input);

        // 输出各个分组内容
        for (List<String> group : groups) {
            System.out.println("分组：");
            for (String line : group) {
                System.out.println(line);
            }
            System.out.println("--------------");
        }
    }
}