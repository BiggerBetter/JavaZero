package lambdaLearn.策略设计模式;

public class FilterEmployeeBySalary implements MyPredicate<Employee>{
    @Override
    public boolean test(Employee employee) {
        return employee.getSalary()>=4000;
    }
}
