package lambdaLearn;

import org.junit.Test;
import 策略设计模式.Employee;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * java8 中最重要的改变 一是lambda表达式 另一个是stream
 * 用stream处理集合 类似于用sql执行数据库查询 高效益与使用的处理数据方式
 * 一。stream的三个操作步骤
 * 1.创建
 * 2.中间操作
 * 3.终止操作
 */

public class testStream {
    List<Employee> employees= Arrays.asList(
            new Employee("zhang3",36,5000),
            new Employee("zhang8",36,7000),
            new Employee("zhang4",40,6000),
            new Employee("zhang5",50,7000),
            new Employee("zhang6",25,9000)
    );
    @Test
    public void test1(){
        //1.通过collection系列集合提供stream()
        List<String> list = new ArrayList<>();
        Stream<String> stream = list.stream();

        //2.通过Arrays中的静态方法Stream获取数据流
        Employee[] employees = new Employee[10];
        int[] a = new int[]{1,2,3,4,5};
        Stream<Employee> stream1 = Arrays.stream(employees);
        IntStream streamInt = Arrays.stream(a);

        //3.stream类中的静态方法of()
        Stream<String> stream2 = Stream.of("q","d");

        //4.创建无限流
        //迭代
        Stream<Integer> stream3 = Stream.iterate(0,(x)->x+2);
        stream3.limit(10).forEach(System.out::println);
        //生成
        Stream.generate(()->Math.random())
                .limit(5)
                .forEach(System.out::println);
    }

    //中间操作 不会有任何结果 需要终止操作
    /**
            筛选与切片
            filter-接受lambda，从流中排除某些元素
            limit-截断流，元素不超过指定数量
            skip(n)-跳过元素，返回一个扔掉了前n个元素的流，若元素不足n个 返回空流
            distinct-筛选，通过流所生成元素的hashcode()和equals去除重复元素
    */
    @Test
    public void test2(){
        employees.stream().filter((e)->e.getAge()>20).forEach(System.out::println);
        System.out.println("===============");
        employees.stream().filter((e)->e.getSalary()>5000).skip(2).forEach(System.out::println);
    }
    //中间操作
    /**
            映射
            map-接收lambda，将元素转换成其他形式或提取信息。接受一个函数作为参数，该函数会被作用到每一个元素上，并将其映射为一个新元素。
            flatmap-接受一个函数作为参数，将流中每个值都换成另一个流，然后把所有流连接成一个流
            理解为扁平化操作 [[1,2,3],[4,5]] 两层次变一个层次[1,2,3,4,5]
     */
    @Test
    public void test3() {
        List<String> list = Arrays.asList("aaa", "bbb", "ccc");
        list.stream().map((str)->str.toUpperCase(Locale.ROOT)).forEach(System.out::println);
        //提取全部名字
        employees.stream()
                .map(Employee::getName)
                .forEach(System.out::println);
    }
    @Test
    public void testMap(){
        //map
        String[] words = new String[]{"hello","world"};
        List<String[]> a = Arrays.stream(words).map((x)->x.split("")).collect(Collectors.toList());
        //a.forEach(System.out::println); //返回含有两个string[]的list
        for(String[] strs:a){
            for(int i = 0 ; i<strs.length;i++){
                System.out.print(strs[i]+" ");
            }
            System.out.println("");
        }
        //flatmap  每个元素变成流并连接在一起
        //map后每个元素都是string[] flatmap后流中每个string[]变成流并连接
        List<String> b = Arrays.stream(words).map((x)->x.split("")).flatMap((x)->Arrays.stream(x)).distinct().collect(Collectors.toList());
        b.stream().forEach(System.out::println);
    }
    /**
        排序
        sorted 自然排序
        sorted(Comparator com) 定制排序
     */
    @Test
    public void test4(){
        List<String> list = Arrays.asList("aaa","bbb","ccc");
        list.stream().sorted().forEach(System.out::println);
        employees.stream().sorted((e1,e2)->Integer.compare(e1.getSalary(),e2.getSalary())).forEach(System.out::println);

        System.out.println("======");

        //定制排序 Comparator
        List<Employee> list1 = employees.stream().sorted((e1,e2)->{
            if (e1.getAge().equals(e2.getAge())) {
                return e1.getSalary().compareTo(e2.getSalary());
            }else return e1.getAge().compareTo(e2.getAge());
        }).collect(Collectors.toList());
        list1.stream().forEach(System.out::println);
    }
    /**
        终止操作
        查找与匹配
            allmatch 检查是否匹配全部元素
            anymatch 检查是否至少匹配一个元素
            findfirst 返回第一个元素 用optional接受 可能会没值
            findany 返回当前流中任意元素
        归约
            reduce
        收集
     */
    @Test
    public void test5() {
        //查找与匹配-----------------------------------------
        boolean a = employees.stream().allMatch((e) -> e.getAge().equals(20));
        System.out.println(a);
        boolean b = employees.stream().allMatch((e) -> e.getAge() > 20);
        System.out.println(b);
    }
    @Test
    public void test6() {
        //findfirst 返回第一个元素 用optional接受
        Optional<Employee> op = employees.stream().sorted(Comparator.comparingInt(Employee::getAge).reversed()).findFirst();
        System.out.println(op.get().getAge());


        //count 流中元素
        long count = employees.stream().count();
        System.out.println(count);


        //max min
        Optional<Employee> op1 = employees.stream().max((e1, e2) -> Integer.compare(e1.getSalary(), e2.getSalary()));
        System.out.println(op1.get().getSalary());
    }
    @Test
    public void test7(){
        //归约-------------------------------------------
        List<Integer> list = Arrays.asList(1,2,3,4,5,6,7,8);
        //累加操作
        int sum = list.stream().reduce(0,(x,y)->x+y);//从0开始加
        //例子 工资累加
        int salary = employees.stream().map((e)->e.getSalary()).reduce(0,(x,y)->x+y);

        //收集---------------------------------------
        employees.stream().map(Employee::getName).collect(Collectors.toList());
        employees.stream().map(Employee::getName).collect(Collectors.toSet());
        employees.stream().map(Employee::getAge).collect(Collectors.toCollection(()->new HashSet<>()));

        //总数
        long count1 = list.stream().collect(Collectors.counting());
        //平均值
        employees.stream().collect(Collectors.averagingInt((e)->e.getSalary()));
        //总和
        employees.stream().collect(Collectors.summarizingInt((e)->e.getSalary()));


        //分组
        Map<Integer,List<Employee>> map = employees.stream().collect(Collectors.groupingBy((e)->e.getSalary()));

        for (Map.Entry<Integer, List<Employee>> entry : map.entrySet()) {
            System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
        }

    }

}
