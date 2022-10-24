package Try;

import Utils.ListNode;

import java.util.ArrayList;
import java.util.List;

public class Try2 {
    public static void main(String[] args) {
        //把一个对象赋值给另一个对象，那引用另一个对象的对象会怎么样


        ListNode head = new ListNode();head.setName("head");head.setValue(0);
        head.setNext(null);
        ListNode tail = new ListNode();tail.setName("tail");tail.setValue(-1);
        head.next = tail;

        ListNode newNode = new ListNode();newNode.setName("newNode");newNode.setValue(1);

        tail.setValue(9);
        tail.setName("changed tail");

        ListNode lnTmp = new ListNode();
        lnTmp.setName("new");
        lnTmp.setValue(1);
        tail.next  = lnTmp;

        tail = lnTmp;


        System.out.println(tail.getName());

    }

}
