package Try;

import ToolClasses.Apple;
import ToolClasses.AppleSon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Try2 {
    public static void main(String[] args) {
//        List<Map<String, Double>> list = new ArrayList<>();
//        f4(list);

        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    System.out.println("第1次获取锁，这个锁是：" + this);
                    int index = 1;
                    while (true) {
                        synchronized (this) {
                            System.out.println("第" + (++index) + "次获取锁，这个锁是：" + this);
                        }
                        if (index == 10) {
                            break;
                        }
                    }
                }
            }
        }).start();
    }

    public static void testApple(){
        List<Map<String, ? super AppleSon>> list = new ArrayList<>();
        a2(list);
    }


    public static void a(List<? extends Map<String, ?>> mapList) {

    }

    public static void a1(List<Map<String, AppleSon>> mapList1) {

    }

    public static void a2(List<Map<String, ? super AppleSon>> mapList1) {
        //可以接收List<Map<String, Double>>
    }

    public static void a3(List<Map<String, ?>> mapList1) {

    }

    public static void a4(List<? extends Map<String, ?>> mapList1) {
        //可以接收List<Map<String, Double>>
    }

    public static void f(List<? extends Map<String, ?>> mapList) {

    }

    public static void f1(List<Map<String, ?>> mapList1) {

    }

    public static <T> void f2(List<Map<String, T>> mapList1) {
        //可以接收List<Map<String, Double>>
    }

    public static void f3(List<Map<String, ?>> mapList1) {

    }

    public static void f4(List<? extends Map<String, ?>> mapList1) {
        //可以接收List<Map<String, Double>>
    }

    public static void ff(int i) {

    }
}
