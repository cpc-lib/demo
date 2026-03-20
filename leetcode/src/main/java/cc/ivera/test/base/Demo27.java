package cc.ivera.test.base;

public class Demo27 {
    public static void main(String[] args) {
        String s = """
                1699451095565	|	1	|	main	|	张三走出餐厅，来到公交站
                1699451095565	|	1	|	main	|	等待 700路 或者 800路 公交到来
                1699451095568	|	16	|	ForkJoinPool.commonPool-worker-1	|	700路公交正在赶来
                1699451095568	|	17	|	ForkJoinPool.commonPool-worker-2	|	800路公交正在赶来
                1699451095673	|	16	|	ForkJoinPool.commonPool-worker-1	|	700路到了
                1699451095673	|	16	|	ForkJoinPool.commonPool-worker-1	|	java.lang.RuntimeException: 撞树了……
                1699451095673	|	16	|	ForkJoinPool.commonPool-worker-1	|	小白叫出租车
                1699451095678	|	1	|	main	|	出租车 叫到了,小白坐车回家
                """;
        System.out.println(s);
    }
}
