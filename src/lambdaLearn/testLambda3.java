package lambdaLearn;

import org.junit.Test;
import lambdaLearn.strategyDesign.Employee;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * java8内置四大函数式接口
 *
 * Consumer<T>:消费型接口
 *      void accept(T t);
 * Supplier<T>:供给型接口
 *      T get();
 * Function<T,R>:函数型接口
 *      R apply(T t);
 * Predicate<T>:断言型接口
 *      boolean test(T t);
 *
 *
 * 方法引用：若lambda体中的内容有方法已经实现了 我们可以使用“方法引用”
 * （可以理解为方法引用是lambda表达式的另一种表现形式）
 * 主要由三种语法格式
 * 对象::实例方法名
 * 类::静态方法名
 * 类::实例方法名
 *
 *     lambda体中调用方法的参数列表和返回值类型 要与 函数式接口的抽象方法的参数列表和返回值类型一致
 *
 * 构造器引用：
 * ClassName::new
 *
 */
public class testLambda3 {
    /**
     * 对象::实例方法名
     */
    @Test
    public void test(){
        //lambda体中有方法已经实现了
        Consumer<String> con = (x)->System.out.println(x);
        //方法引用方式 对象::实例方法名
        PrintStream ps=System.out;
        Consumer<String> c = ps::println;
        //
        Consumer<String> c1= System.out::println;
        c1.accept("123");
    }
    @Test
    public void test1(){
        Employee e =new Employee("1",12,30000);
        Supplier<String> s = ()->e.getName();
        System.out.println(s.get());
        //对象::实例名
        Supplier<Integer> s2 = e::getSalary;
        System.out.println(s2.get());
    }

    /**
     * 类::静态方法名
     */
    @Test
    public void test2(){
        Comparator<Integer> com = (x,y)->Integer.compare(x,y);
        Comparator<Integer> com1 = Integer::compare;// 1 , 0, -1
        System.out.println(com.compare(4,3));
    }

    /**
     * 第一个参数是实例方法调用者 第二个参数是实例方法的参数时 可以使用
     * 类::实例方法名
     */
    @Test
    public void test3(){
        BiPredicate<String,String> bp=(x,y)->x.equals(y);
        //第一个参数是实例方法调用者 第二个参数是实例方法的参数时 可以使用
        BiPredicate<String,String> bp1 = String::equals;
        System.out.println(bp1.test("123","123"));
    }

    /**
     * 构造器引用
     *
     */
    @Test
    public void test4(){
        //调用哪个构造器 取决于函数是接口中有几个参数 就调用几个参数的构造器
        Supplier<Employee> sup=()->new Employee();
        //无参
        sup.get();
        Supplier<Employee> sup1 = Employee::new;
    }
}
