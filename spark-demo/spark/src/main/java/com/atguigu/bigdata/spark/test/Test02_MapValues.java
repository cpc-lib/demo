package com.atguigu.bigdata.spark.test;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

import java.util.Arrays;

public class Test02_MapValues {
    public static void main(String[] args) {

        // 1.创建配置对象
        SparkConf conf = new SparkConf().setMaster("local[*]").setAppName("sparkCore");

        // 2. 创建sparkContext
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 3. 编写代码
        JavaPairRDD<String, String> javaPairRDD = sc.parallelizePairs(
                Arrays.asList(
                        new Tuple2<>("k", "v"),
                        new Tuple2<>("k1", "v1"),
                        new Tuple2<>("k2", "v2")));

        // 只修改value 不修改key
        JavaPairRDD<String, String> mapValuesRDD = javaPairRDD.mapValues(new Function<String, String>() {
            @Override
            public String call(String v1) throws Exception {
                return v1 + "|||";
            }
        });

        mapValuesRDD.collect().forEach(System.out::println);

        // 4. 关闭sc
        sc.stop();
    }
}