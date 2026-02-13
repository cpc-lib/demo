package com.atguigu.bigdata.spark.test;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.Arrays;

public class Test05_SortByKey {
    public static void main(String[] args) {
        // 1.创建配置对象
        SparkConf conf = new SparkConf().setMaster("local[*]").setAppName("sparkCore");

        // 2. 创建sparkContext
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 3. 编写代码
        JavaPairRDD<Integer, String> javaPairRDD = sc.parallelizePairs(Arrays.asList(new Tuple2<>(4, "a"),new Tuple2<>(2, "d"), new Tuple2<>(3, "c")));

        // 填写布尔类型选择正序倒序
        JavaPairRDD<Integer, String> pairRDD = javaPairRDD.sortByKey(true);

        pairRDD.collect().forEach(System.out::println);

        // 4. 关闭sc
        sc.stop();
    }
}
