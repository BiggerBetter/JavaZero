package Wanting;

import java.util.ArrayList;
import java.util.Arrays;

public class ArrayBase {
    public static void main(String[] args) {
        //新建的两种方法：1
        int[] nums = new int []{1,2};
        //新建的两种方法：2
        int[] nums2 = {1,2};
        //获取元素数量
        int length = nums.length;
        //转成ArrayList
        ArrayList<String> alist = new ArrayList<>();
        alist = new ArrayList<>(Arrays.asList("1","2"));


        System.out.println(length);
    }
}
