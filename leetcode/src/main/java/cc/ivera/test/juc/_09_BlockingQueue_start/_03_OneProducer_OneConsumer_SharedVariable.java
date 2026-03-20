package cc.ivera.test.juc._09_BlockingQueue_start;


import cc.ivera.test.juc.SmallTool;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class _03_OneProducer_OneConsumer_SharedVariable {
    public static void main(String[] args) {
        final int count = 1200;
        Queue<String> shaobingQueue = new LinkedList<>();

        List<String> xiaoBaiMsg = new LinkedList<>();
        List<String> roadPeopleAMsg = new LinkedList<>();

        Thread xiaoBai = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                String tmp = String.format("第%d个烧饼", i + 1);
                shaobingQueue.add(tmp);
                xiaoBaiMsg.add(String.format("%d 小白制作了 [%s]，当前数量 %d", System.currentTimeMillis(), tmp, shaobingQueue.size()));
            }
        });

        Thread roadPeopleA = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                roadPeopleAMsg.add(String.format("%d  路人甲 买到了 [%s]", System.currentTimeMillis(), shaobingQueue.poll()));
            }
        });

        xiaoBai.start();
        roadPeopleA.start();

        try {
            xiaoBai.join();
            roadPeopleA.join();
        } catch (InterruptedException e) {
            SmallTool.printTimeAndThread("join 产生中断" + e.getMessage());
        }

        List<String> xiaoBaiMsgSub = xiaoBaiMsg.subList(xiaoBaiMsg.size() - 1, xiaoBaiMsg.size());
        System.out.println(xiaoBaiMsgSub.stream().collect(Collectors.joining("\n")));
        System.out.println("--------------------------");   // 分隔线
        List<String> roadPeopleAMsgSub = roadPeopleAMsg.subList(roadPeopleAMsg.size() - 5, roadPeopleAMsg.size());
        System.out.println(roadPeopleAMsgSub.stream().collect(Collectors.joining("\n")));
    }
}
