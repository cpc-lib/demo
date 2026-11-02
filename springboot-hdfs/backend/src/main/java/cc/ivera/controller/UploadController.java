
package cc.ivera.controller;

import cc.ivera.service.ChunkUploadService;
import cc.ivera.service.HdfsService;
import com.example.hdfsbackend.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/upload")
public class UploadController {

    private final ChunkUploadService chunk;
    private final HdfsService hdfs;

    @Value("${HDFS_HTTP_HOST:127.0.0.1}")
    private String hdfsHttpHost;

    @Value("${HDFS_HTTP_PORT:9870}")
    private String hdfsHttpPort;

    public UploadController(ChunkUploadService c,HdfsService h){
        this.chunk=c; this.hdfs=h;
    }

    @PostMapping("/check")
    public boolean check(@RequestParam String uploadId,@RequestParam int chunkIndex){
        return chunk.check(uploadId,chunkIndex);
    }

    @PostMapping("/chunk")
    public String chunk(@RequestParam("file") MultipartFile f,
                        @RequestParam String uploadId,
                        @RequestParam int chunkIndex) throws Exception {
        chunk.save(f.getInputStream(),uploadId,chunkIndex);
        return "ok";
    }

    @PostMapping("/merge")
    public Map<String,Object> merge(@RequestParam String uploadId,
                        @RequestParam int totalChunks,
                        @RequestParam String hdfsPath) throws Exception {
        File m=chunk.merge(uploadId,totalChunks);
        hdfs.upload(new FileInputStream(m),hdfsPath);
        chunk.clean(uploadId);

        // HDFS 访问地址从环境变量读取
        String hdfsViewUrl = "http://" + hdfsHttpHost + ":" + hdfsHttpPort + "/webhdfs/v1" + hdfsPath + "?op=OPEN";

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "merge ok");
        result.put("hdfsUrl", hdfsViewUrl);
        return result;
    }

    @GetMapping("/download")
    public void download(@RequestParam String hdfsPath, HttpServletResponse resp) throws Exception {
        hdfs.download(hdfsPath, resp);
    }
}
