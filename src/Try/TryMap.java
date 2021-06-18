package Try;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TryMap {
    public static void main(String[] args) {
        Map<String,String> map1 = new HashMap<>();
        map1.put("a","aa");
        Map<String,String> map2 = new HashMap<>();
        map2.put("b","bb");
        map2.put("c","cc");

        map1.putAll(map2);

        Set<Map.Entry<String, String>> entrySet = map1.entrySet();

        String replaced = map1.replace("a","aaa");

        System.out.println(map1.get("a"));


    }

    private static class CNM<K,V,Z>{
        final K key;
        V value;
        private CNM(K key) {
            this.key = key;
        }
    }

}
