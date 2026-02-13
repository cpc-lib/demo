package cc.ivera.split;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SideOutputDataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class SplitByFilterDemo2 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        env.setParallelism(2);

        DataGeneratorSource<String> genSource = new DataGeneratorSource<>(
                (GeneratorFunction<Long, String>) value -> "Number:" + value,
                10,
                RateLimiterStrategy.perSecond(1),
                Types.STRING
        );

        // 1) Source -> Stream（关键）
        DataStreamSource<String> stream = env.fromSource(
                genSource,
                org.apache.flink.api.common.eventtime.WatermarkStrategy.noWatermarks(),
                "datagen"
        );

        // 2) 定义侧输出：奇数
        OutputTag<String> oddTag = new OutputTag<>("odd", Types.STRING);

        // 3) 主流输出偶数，侧输出输出奇数
        SingleOutputStreamOperator<String> evenStream = stream.process(new ProcessFunction<String, String>() {
            @Override
            public void processElement(String value, Context ctx, Collector<String> out) {
                // value: "Number:9"
                long num = Long.parseLong(value.substring(value.indexOf(':') + 1));
                if (num % 2 == 0) {
                    out.collect(value); // 偶数走主流
                } else {
                    ctx.output(oddTag, value); // 奇数走侧输出
                }
            }
        });

        SideOutputDataStream<String> oddStream = evenStream.getSideOutput(oddTag);

        evenStream.print("偶数流");
        oddStream.print("奇数流");

        env.execute();
    }
}
