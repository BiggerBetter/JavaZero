package Try;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TryQuickSort {
    public static void main(String[] args) {
        List<Integer> sourceList = new ArrayList<>(Arrays.asList(1,3,5,7,2,4,6));
        System.out.println("使用stream排序："+ sourceList.stream().sorted().collect(Collectors.toList()));








    }
}
