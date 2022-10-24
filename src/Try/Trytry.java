package Try;

import Utils.ListNode;

import java.util.ArrayList;
import java.util.List;

public class Trytry {
    public static void main(String[] args) {
        int[] l1 = {9, 9, 9, 9, 9, 9, 9};
        int[] l2 = {9, 9, 9, 9};
        int target = 9;


    }

    public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();
//        List<Integer> list3 = new ArrayList<>();

        while (l1 != null) {
            list1.add(l1.getValue());
            l1 = l1.next;
        }

        while (l2 != null) {
            list2.add(l2.getValue());
            l2 = l2.next;
        }

        int numSize1 = list1.size();
        int numSize2 = list2.size();

        int numSize3 = Math.max(list1.size(), list2.size());
        //todo 疑问，如果是string 的list的花，我get出来改了会影响本来的list吗
        int[] array3 = new int[numSize3 + 1];

        int point1 = numSize1 - 1;
        int point2 = numSize2 - 1;
        int upMark = 0;
        for (int i = 0; i < numSize3; i++) {
            int tmpRes = 0;
            if (point1 >= i) {
                int num = list1.get(point1);
                tmpRes += num;
            }

            if (point2 >= i) {
                int num = list2.get(point2);
                tmpRes += num;
            }
            if (upMark == 1) {
                tmpRes += 1;
                upMark = 0;
            }

            if (tmpRes > 9) {
                tmpRes = tmpRes - 10;
                upMark = 1;
            }
            array3[numSize3 - 1 - i] = tmpRes;
        }
        if (upMark == 1) {
            array3[0] = 1;
        }

        int startMark = 0;
        if (array3[0] == 0) {
            startMark = 1;
        }

        ListNode head = new ListNode();
        ListNode tail = new ListNode();
        head.next = tail;


        if (startMark == 1) {
            for (int k = 1; k < numSize3 + 1; k++) {
                if (k == 1) {
                    head.setValue(array3[k]);
                } else {
                    tail.setValue(array3[k]);
                    ListNode lnTmp = new ListNode();
                    tail.next  = lnTmp;
                    tail = lnTmp;
                }
            }
        }else {
            for (int k = 0; k < numSize3 + 1; k++) {
                if (k == 0) {
                    head.setValue(array3[k]);
                } else {
                    tail.setValue(array3[k]);
                    ListNode lnTmp = new ListNode();
                    tail.next  = lnTmp;
                    tail = lnTmp;
                }

            }
        }
    return head;
    }


}
