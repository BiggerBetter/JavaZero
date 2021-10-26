package lambdaLearn.策略设计模式;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class testLambda {
    List<Employee> employees= Arrays.asList(
            new Employee("zhang3",36,5000),
            new Employee("zhang4",40,6000),
            new Employee("zhang5",50,7000),
            new Employee("zhang6",25,9000)
    );

    /*
     过滤年龄
     */
    @Test
    public void test(){
        List<Employee> result = new ArrayList<>();
        for(Employee e :employees){
            if(e.getAge()>35)
                result.add(e);
        }
        result.stream().forEach(System.out::println);

//        List<Employee> result = new ArrayList<>();
//        for(Employee e :employees){
//            if(e.getSalary()>50000)
//                result.add(e);
//        }
//        result.stream().forEach(System.out::println);
    }
    /**
     *优化方法1 策略模式
     */
    @Test
    public void test1() {
        //将不同过滤规则的实例传入
        //按照年龄过滤
        List<Employee> list1 = filter(employees, new FilterEmployeeByAge());
        list1.stream().forEach(System.out::println);
        //按照薪资过滤
        List<Employee> list2 = filter(employees, new FilterEmployeeBySalary());
    }
    /**
     * 只需要写一个方法
     * @param list 要过滤的列表
     * @param mp 要过滤使用的规则
     * @return
     */
    public List<Employee> filter(List<Employee> list , MyPredicate<Employee> mp){
        List<Employee> re = new ArrayList<>();
        for(Employee employee : list){
            if(mp.test(employee)){
                re.add(employee);
            }
        }
        return re;
    }
    /**
     * 优化方法2 匿名内部类
     */
    @Test
    public void test2(){
        List<Employee> list3=filter(employees, new MyPredicate<Employee>() {
            @Override
            public boolean test(Employee employee) {
                return employee.getSalary()>=10000;
            }
        });
    }

    /**
     * 优化方法3 lambda表达
     */
    @Test
    public void test3(){
        List<Employee> list4 = filter(employees,(e)->e.getSalary()>=2000);
        list4.forEach(System.out::println);
    }

    /**
     * 优化方法4 stream流+lambda表达
     */
    @Test
    public void test4(){
        employees.stream()
                .filter((e)->e.getSalary()<=10000)
                .forEach(System.out::println);
    }


}
