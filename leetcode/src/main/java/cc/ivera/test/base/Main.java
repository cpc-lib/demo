package cc.ivera.test.base;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int n = sc.nextInt(); // 初始烟数
        int k = sc.nextInt(); // k 个烟蒂换 1 根烟

        System.out.println(maxSmoke(n, k));
    }

    /**
     * 计算最多能抽多少根烟（循环模拟）
     */
    private static int maxSmoke(int n, int k) {
        if (n <= 0 || k <= 1) return n;

        int total = 0;   // 总共抽了多少根
        int butts = 0;   // 当前烟蒂数

        int cigarettes = n;
        while (cigarettes > 0) {
            total += cigarettes;       // 当前烟抽完
            butts += cigarettes;       // 烟蒂累计

            cigarettes = butts / k;    // 用烟蒂换来的新烟
            butts = butts % k;         // 换完后剩下的烟蒂
        }

        return total;
    }
}
