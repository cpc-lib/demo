package cc.ivera.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.net.URI;

@org.springframework.context.annotation.Configuration
public class HadoopConfig {

    @Value("${hdfs.default-fs}")
    private String fsUri;

    @Value("${hdfs.user}")
    private String user;

    @Bean
    public FileSystem fileSystem() throws Exception {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", fsUri);
        return FileSystem.get(new URI(fsUri), conf, user);
    }
}
