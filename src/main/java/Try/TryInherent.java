//package Try;
//
//import ToolClasses.Apple;
//import ToolClasses.Fruit;
//import ToolClasses.Plant;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class TryInherent {
//    public static void main(String[] args) {
//        Apple apple = new Apple();
//        Fruit fruit = new Fruit();
//        Plant plant = new Plant();
//        Object ob = new Object();
//
//        //super
//        List<? super Fruit> superList = new ArrayList<>();
//
//        superList.add(plant);
//        superList.add(fruit);
//        superList.add(apple);
//        superList.add(ob);
//        superList.add(null);
//
//        //sub
//        List<? extends Fruit> subList = new ArrayList<>();
//
//        subList.add(plant);
//        subList.add(fruit);
//        subList.add(apple);
//        subList.add(ob);
//        subList.add(null);
//    }
//
//
//    List<? extends A> subList = new ArrayList<>();
//
//        subList.add(new A());
//
//}
