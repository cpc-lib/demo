
package cc.ivera.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.net.URI;

@org.springframework.context.annotation.Configuration
public class HadoopConfig {

    @Value("${hadoop.fs-uri}")
    private String fsUri;

    @Value("${hadoop.user}")
    private String user;

    @Bean
    public FileSystem fs() throws Exception {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", fsUri);
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
        conf.set("dfs.client.use.datanode.hostname", "true");
        return FileSystem.get(new URI(fsUri), conf, user);
    }
}
