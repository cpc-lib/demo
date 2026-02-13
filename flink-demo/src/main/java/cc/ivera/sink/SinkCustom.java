package cc.ivera.sink;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

/**
 * TODO
 *
 * @author cjp
 * @version 1.0
 */
public class SinkCustom {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        SingleOutputStreamOperator<String> sensorDS = env
                .socketTextStream("hadoop102", 7777);


        sensorDS.addSink(new MySink());


        env.execute();
    }

    public static class MySink extends RichSinkFunction<String> {

        private transient BufferedWriter writer;
        private transient Path filePath;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);

            // 相对路径：以 Job 运行时的工作目录为基准
            filePath = Paths.get("output", "word.txt");

            // 确保 output 目录存在
            Files.createDirectories(filePath.getParent());

            // 追加写入（append=true），UTF-8 编码
            writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(filePath.toFile(), true),
                            StandardCharsets.UTF_8
                    )
            );
        }

        @Override
        public void close() throws Exception {
            // 关闭前 flush + close
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            super.close();
        }

        /**
         * 来一条数据调用一次：这里只做写入，不要创建连接/IO对象
         */
        @Override
        public void invoke(String value, Context context) throws Exception {
            writer.write(value);
            writer.newLine();

            // 简单起见：每条都 flush（吞吐低一些，但最直观）
            writer.flush();
        }
    }
}
