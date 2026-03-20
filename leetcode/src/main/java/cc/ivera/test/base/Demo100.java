package cc.ivera.test.base;

import java.util.LinkedList;
import java.util.Queue;

public class Demo100 {
    public static void main(String[] args) {
        Queue<String> queue = new LinkedList<String>();
        queue. offer("string"); // add
        System. out. println(queue. poll());
        System. out. println(queue. remove());
        System. out. println(queue. size());
    }
}
