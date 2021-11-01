package lambdaLearn.strategyDesign;

public class FilterEmployeeBySalary implements MyPredicate<Employee>{
    @Override
    public boolean test(Employee employee) {
        return employee.getSalary()>=4000;
    }
}
