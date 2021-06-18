package Try;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TrySet {
    public static void main(String[] args) {
        Set<String> set1 = new HashSet<>(Arrays.asList("aa", "bb", "cc"));
        set1.add("dd");
        Set<String> set2 = new HashSet<>(Arrays.asList("ee", "ff"));
        set1.addAll(set2);

        set1.remove("aa");

        System.out.println(set1);

        //todo:entry

    }
}
