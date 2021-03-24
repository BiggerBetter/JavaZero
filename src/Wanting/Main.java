package Wanting;

import Utils.Apple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        List<Apple> appleList = new ArrayList<>();
        Apple apple1 = new Apple(1,2,"a");
        Apple apple2 = new Apple(2,1,"b");
        Apple apple3 = new Apple(2,2,"a");

        appleList.add(apple1);
        appleList.add(apple2);
        appleList.add(apple3);

        //先以字符串作为level1 key，先以数字和作level2 key
        Map<String,List<Apple>> map = appleList.stream().collect(Collectors.groupingBy(Apple::getIs));
        Map<String,Map<Integer,List<Apple>>> map1 = new HashMap<>();
        map.forEach((k,v) -> map1.put(k,v.stream().collect(Collectors.toMap(Apple::getId))));

        System.out.println(map1);

    }

}
