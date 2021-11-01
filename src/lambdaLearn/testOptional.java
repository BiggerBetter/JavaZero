package lambdaLearn;

import org.junit.Test;
import lambdaLearn.strategyDesign.Employee;

import java.util.*;
import java.util.stream.Collectors;

public class testOptional {
    /**
     * optional<T> 是一个容器类 代表一个值存在或者不存在 原来是用null
     * 现在用此容器类可以更好的表达这个概念 并且避免空指针异常
     */

    List<Employee> employees= Arrays.asList(
            new Employee("zhang3",36,5000),
            new Employee("zhang4",40,6000),
            new Employee("zhang5",50,7000),
            new Employee("zhang6",25,9000),
            new Employee("zhang6",23,9000),
            new Employee("zhang6",21,9000),
            new Employee("zhang6",20,9000));
    /*
    用optional包装 防止空指针异常
    对于一个返回值 可以有值 可能没有值 用optional 方法灵活处理
     */
    @Test
    public void test(){
        //创建一个带泛型的optional实例
        Optional<Employee> op = Optional.of(new Employee());
        //获取实例
        op.get();
        //创建一个空optional实例
        Optional.empty();
        //ofNullable(T t) 若t不为null 创建实例 否则返回空实例
        Employee E =null;
        Optional<Employee> op1 = Optional.ofNullable(E);
        //判断是否有值
        System.out.println(op1.isPresent());

        //orElse(T t) 如果调用容器有值 返回容器中的值 否则返回空实例
        Employee employee = op.orElse(new Employee("zhan",30,1344));//op有值 返回容器中的值
        Employee employee1 = op1.orElse(new Employee("zhan",30,1344));//容器中没值 返回默认值
        System.out.println(employee);
        System.out.println(employee1);

        //orElseGet(Supplier s)
        op.orElseGet(()->{
            int salary=0;
            salary=employee.getSalary()+employee1.getSalary();
            return new Employee("sum",20,salary);
        });

        //map(Function f) 对容器中的值处理 没值返回Optional.empty()
        Optional<String> str = op.map((e)->e.getName());
        System.out.println(str.get());

        //flatmap 要求返回对象必须是Optional 进一步防止空指针异常
        Optional<Integer> integer = op.flatMap((e)->Optional.of(e.getSalary()));
    }

    /*
    举例
        man可能有godness  但是godness一定有名字
     */
    @Test
    public void test1(){
        Man man = new Man();
        String n = getGodnessName(man);
        String n1 = getGodnessName1(Optional.ofNullable(new NewMan()));
    }

    public String getGodnessName(Man man){
        //return man.getGodnessName().getName();空指针异常
        if(man!=null){
            Godness gn = man .getGodness();
            if(gn!=null){
                return gn.getName();
            }
        }
        return "默认值";
    }

    public String getGodnessName1(Optional<NewMan> man){
        return man.orElse(new NewMan()).getGodness().orElse(new Godness("默认值")).getName();
    }
}
