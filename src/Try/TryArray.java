package Try;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TryArray {
    public static void main(String[] args) {
        //新建的两种方法：1
        int[] nums = new int []{1,2};

        //新建的两种方法：2
        int[] nums2 = {1,2};

        //Java里面有字符串类型的数组。Java的数组有一点基本类型的味道
        String[] strArray = {"1","2"};

        //获取元素数量
        int length = nums.length;

        //新建ArrayList，Arrays.asList("1","2")返回的是List抽象类不能自动转换为ArrayList，得用new ArrayList<>()构造函数
        ArrayList<String> alist = new ArrayList<>(Arrays.asList("1","2"));

        //取数据
        String out = strArray[0];

        //字符串取字符,结果不能说String，得是char
        char cha = out.charAt(0);

        //Character 是char的包装类，能自动拆包、打包
        Character character = out.charAt(0);

        System.out.println("hh");
    }
}

/**
 * C语言中的数组：
 * double balance[5] = {1000.0, 2.0, 3.4, 7.0, 50.0};
 *
 */