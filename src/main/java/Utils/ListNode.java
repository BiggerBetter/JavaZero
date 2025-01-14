package Utils;

public class ListNode {
    public int getValue() {
        return Value;
    }

    public void setValue(int Value) {
        this.Value = Value;
    }

    public ListNode getNext() {
        return next;
    }

    public void setNext(ListNode next) {
        this.next = next;
    }

    int Value;
    public ListNode next;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String name;

    public ListNode() {
        this.name = null;
        this.Value =-1;
        this.next = null;
    }

    ListNode(int Value) {
        this.Value = Value;
    }

    ListNode(int Value, ListNode next) {
        this.Value = Value;
        this.next = next;
    }
}
