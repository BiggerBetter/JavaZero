package Utils;

import java.util.*;

public class FormatString {
    static String input = "{[0,0,4,1],[7,0,8,2],[6,2,8,3],[5,1,6,3],[4,0,5,1],[6,0,7,2],[4,2,5,3],[2,1,4,3],[0,1,2,2],[0,2,2,3],[4,1,5,2],[5,0,6,1]}";

    public static void main(String[] args) {
        String str = input;
        //遇到一个括号,深度加一,直到不是括号,定型字符串数据结构层次,开始构建数据结构
        Map<String, String> braceMap = new HashMap<>();
        braceMap.put("(",")");
        braceMap.put("{","}");
        braceMap.put("[","]");
        braceMap.put("【","】");
        braceMap.put("「","」");

        Set<String> leftSet = braceMap.keySet();

        List<String> stack = new ArrayList<>();

        List<String> tmpList1 = new ArrayList<>();
        List<List<String>> tmlList2 = new ArrayList<>();
        List<List<List<String>>> tmlList3 = new ArrayList<>();

        for (int i = 0; i < str.length(); i++) {
            String tmpChar = subI(str, i);
            if (leftSet.contains(tmpChar)) {
                stack.add(tmpChar);
                stack.add(braceMap.get(tmpChar));
                continue;
            }
            if (tmpChar.equals(stack.get(stack.size() - 1))) {
                tmlList2.add(tmpList1);
                tmpList1 = new ArrayList<>();
                stack = stack.subList(0, stack.size() - 2);
                continue;
            }
            if (!tmpChar.equals(",")) {
                tmpList1.add(tmpChar);
            }

        }
        for (List<String> a : tmlList2) {
            for (String aa : a) {
                System.out.print(aa);
                System.out.print(",");
            }
            System.out.println("");
        }

    }

    private static String subI(String str, int i) {
        return String.valueOf(str.charAt(i));
    }

    private static int[][] formatString2Array2(List<List<String>> list2){
        int l1 =list2.size();
        int l2 =list2.get(0).size();
        int[][] res = new int[l1][l2];
        for (int i = 0; i < l1; i++) {
            for (int j = 0; j < l2; j++) {
                res[i][j] = Integer.parseInt(list2.get(i).get(j));
            }
        }
        return res;
    }
}
