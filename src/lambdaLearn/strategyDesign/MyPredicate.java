package lambdaLearn.strategyDesign;

/**
 * 带泛型接口 
 * @param <T>
 */
@FunctionalInterface
public interface MyPredicate<T>{
    boolean test(T t);
}
