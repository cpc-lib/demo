package com.atguigu.bigdata.spark.rdd.operate;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import java.util.Arrays;

public class Spark02_Operate_Transform_Map_2 {
    public static void main(String[] args) {

        final SparkConf conf = new SparkConf();
        conf.setMaster("local");
        conf.setAppName("spark");

        final JavaSparkContext jsc = new JavaSparkContext(conf);

        jsc
                .parallelize(Arrays.asList(1, 2, 3, 4), 2)
                .map(NumberTest::mul2)
                .collect()
                .forEach(System.out::println);

        jsc.close();

    }
}
