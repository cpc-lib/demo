package cc.ivera.aggreagte;

import cc.ivera.bean.WaterSensor;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class KeybyDemoMax {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        // 关键：有界流用 BATCH，最终只输出一次结果
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);

        DataStreamSource<WaterSensor> sensorDS = env.fromElements(
                new WaterSensor("s1", 1L, 1),
                new WaterSensor("s1", 11L, 11),
                new WaterSensor("s2", 2L, 2),
                new WaterSensor("s3", 3L, 3)
        );

        // (id, 1)
        SingleOutputStreamOperator<Tuple2<String, Long>> idAndOne = sensorDS
                .map((MapFunction<WaterSensor, Tuple2<String, Long>>) v -> Tuple2.of(v.getId(), 1L))
                .returns(Types.TUPLE(Types.STRING, Types.LONG));

        // (id, count)
        SingleOutputStreamOperator<Tuple2<String, Long>> countById = idAndOne
                .keyBy((KeySelector<Tuple2<String, Long>, String>) v -> v.f0)
                .sum(1);

        // 全局取 count 最大的那条 => (id, maxCount)
        SingleOutputStreamOperator<Tuple2<String, Long>> top1 = countById
                .keyBy(v -> 1)     // 常量 key：汇总到一起
                .maxBy(1);         // 按 count 取最大

        top1.print("top1(id,count)");

        env.execute();
    }
}
