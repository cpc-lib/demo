package com.atguigu.bigdata.sparkstreaming;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.StreamingContextState;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import java.net.URI;

public class SparkStreaming08_Close_1 {
    public static void main(String[] args) throws Exception {

        SparkConf conf = new SparkConf();
        conf.setMaster("local[*]");
        conf.setAppName("SparkStreaming");
        final JavaStreamingContext jsc = new JavaStreamingContext(conf, new Duration(3 * 1000));

        // TODO 通过环境对象对接Socket数据源，获取数据模型，进行数据处理
        final JavaReceiverInputDStream<String> socketDS = jsc.socketTextStream("localhost", 9999);
        socketDS.print();

        jsc.start();

        new Thread(new MonitorStop(jsc)).start();

        jsc.awaitTermination(); // await方法表示当前代码执行到此处后会阻塞直到采集器的中止（结束）


    }


    public static class MonitorStop implements Runnable {

        JavaStreamingContext javaStreamingContext = null;

        public MonitorStop(JavaStreamingContext javaStreamingContext) {
            this.javaStreamingContext = javaStreamingContext;
        }

        @Override
        public void run() {
            try {
                FileSystem fs = FileSystem.get(new URI("hdfs://hadoop102:8020"), new Configuration(), "atguigu");
                while (true) {
                    Thread.sleep(5000);
                    boolean exists = fs.exists(new Path("hdfs://hadoop102:8020/stopSpark"));
                    if (exists) {
                        StreamingContextState state = javaStreamingContext.getState();
                        // 获取当前任务是否正在运行
                        if (state == StreamingContextState.ACTIVE) {
                            // 优雅关闭
                            javaStreamingContext.stop(true, true);
                            System.exit(0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


