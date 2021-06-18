package Try;

import java.util.stream.Stream;

public class TryStream {
    public static void main(String[] args) {
        int result1 = Stream.of(1, 2)
                .reduce(2, (v1, v2) -> v1 * v2, (v1, v2) -> v1 * v2);;
        System.out.println(result1);
    }
}
