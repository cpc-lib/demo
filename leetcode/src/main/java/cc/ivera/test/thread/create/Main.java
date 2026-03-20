package cc.ivera.test.thread.create;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author e2607
 * @version 1.0
 * @description: 多线程的创建执行不同的任务
 * @date 11/27/2021 10:52 AM
 */
public class Main {

    public static void main(String[] args) {

        CreateThreadMethodA();
        System.out.println("------------------------");

        CreateThreadMethodB();
        System.out.println("------------------------");

        try {
            CreateThreadMethodC();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    private static void CreateThreadMethodA() {
        Thread threadA = new CreateThreadMethodA();
        threadA.start();
        System.out.println(threadA.getName());
    }

    private static void CreateThreadMethodB() {
        Thread threadB = new Thread(new CreateThreadMethodB());
        threadB.start();
        System.out.println(threadB.getName());
    }

    private static void CreateThreadMethodC() throws InterruptedException, ExecutionException {
        FutureTask futureTask = new FutureTask(new CreateThreadMethodC());
        Thread threadC = new Thread(futureTask);
        threadC.start();
        System.out.println(futureTask.get());
    }
}
