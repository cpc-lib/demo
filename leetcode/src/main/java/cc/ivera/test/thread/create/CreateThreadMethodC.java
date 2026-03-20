package cc.ivera.test.thread.create;

import java.util.concurrent.Callable;

/**
 * @author e2607
 * @version 1.0
 * @description: 实现Callable接口实现多线程
 * @date 11/27/2021 10:52 AM
 */
public class CreateThreadMethodC implements Callable {

    //call方法能够返回数据

    @Override
    public Object call() throws Exception {
        printLove();
        return "我只是欣赏你";
    }

    private void printLove() {
        for (int i = 0; i < 3000; i++) {
            System.out.println("I love you!---" + (i + 1));
        }
    }
}
