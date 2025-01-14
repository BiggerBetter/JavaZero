package lambdaLearn.strategyDesign;

/**
 * 实现接口 按年龄过滤
 */
public class FilterEmployeeByAge implements MyPredicate<Employee>{
    @Override
    public boolean test(Employee employee) {
        return employee.getAge()>=35;
    }
}
