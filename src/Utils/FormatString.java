package Utils;

import java.util.*;

public class FormatString {
    static String input = "[[0,0,4,1],[7,0,8,2],[6,2,8,3],[5,1,6,3],[4,0,5,1],[6,0,7,2],[4,2,5,3],[2,1,4,3],[0,1,2,2],[0,2,2,3],[4,1,5,2],[5,0,6,1]]";

    public static void main(String[] args) {
//        int[] l1 = new int[]{1,2,3};
        List<String> l1 = new ArrayList<>(Arrays.asList("1","2"));

    }


    public static <T> T[] LeetCode2Array(String arg) {
        //从一个括号的到它的对应闭合括号
        //如果继续有括号就多建一层数据结构
        //数组是不能变的，所以在最初创建的时候旧必须规划好，层次，熟练，或者才掌握了全部信息之后再创建
        //这里要兼容多层的情况
        int l = 0;
        for(int i =0; i<arg.length();i++){
            List<String> tmpList = new ArrayList<>();   

        }



    }

    private Map<String,String>  braceSet() {
        Map<String,String>  braceMap = new HashMap();
        braceMap.put("(",")");
        braceMap.put("{","}");
        braceMap.put("[","]");
        braceMap.put("【","】");
        braceMap.put("「","」");
        return braceMap;
    }
}
