package cc.ivera.test.juc._02_createThread;


import java.util.concurrent.*;

public class _03_CreateThread_FutureTask {
    public static void main(String[] args) {
        //use callable to return value
        Callable<String> callable = () -> {
            System.out.println("我是子任务");
            return "sub task done";
        };
        //
        FutureTask<String> task = new FutureTask(callable);
        Thread thread = new Thread(task);
        thread.start();
        System.out.println("子线程启动");
        try {
            //once the task is cancelled,you cannot use task get value returned
            //task.cancel(true);
            String subResult = task.get(6000, TimeUnit.SECONDS);
            System.out.println("子线程返回值：" + subResult);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        System.out.println("main 结束");
    }
}
