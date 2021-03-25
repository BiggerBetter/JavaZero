package Wanting;

import java.util.stream.Stream;

public class testStream {
    public static void main(String[] args) {
        int result1 = Stream.of(1, 2, 3, 4, 5)
                .reduce(1, (v1, v2) -> v1 * v2, (v1, v2) -> v1 * v2);;
        System.out.println(result1);


    }
}
