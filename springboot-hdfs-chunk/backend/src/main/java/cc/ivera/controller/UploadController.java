package cc.ivera.controller;

import cc.ivera.service.HdfsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private final HdfsService hdfs;

    public UploadController(HdfsService hdfs) { this.hdfs = hdfs; }

    @CrossOrigin
    @GetMapping("/md5Check")
    public String md5Check(@RequestParam String md5) {
        String path = hdfs.getPathByMd5(md5);
        return path != null ? path : "";
    }

    @CrossOrigin
    @PostMapping("/check")
    public boolean check(@RequestParam String uploadId, @RequestParam int chunkIndex) throws Exception {
        return hdfs.chunkExists(uploadId, chunkIndex);
    }

    @CrossOrigin
    @PostMapping("/chunk")
    public String chunk(@RequestParam("file") MultipartFile file,
                        @RequestParam String uploadId,
                        @RequestParam int chunkIndex) throws Exception {

        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            System.err.println("空分片 chunkIndex = " + chunkIndex);
            return "empty-skip";
        }

        // 本地目录：Windows/Linux 均支持
        File dir = new File(System.getProperty("java.io.tmpdir") + "/upload/" + uploadId);
        if (!dir.exists()) dir.mkdirs();

        File outFile = new File(dir, "chunk-" + chunkIndex);

        System.out.println("写入本地 chunk：" + outFile.getAbsolutePath());

        try (FileOutputStream fos = new FileOutputStream(outFile);
             InputStream in = file.getInputStream()) {

            byte[] buf = new byte[8 * 1024 * 1024]; // 8MB buffer
            int len;
            while ((len = in.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        }

        // 再次检查是否写入成功
        if (outFile.length() <= 0) {
            outFile.delete();
            return "empty-skip";
        }

        return "ok";
    }

    @CrossOrigin
    @PostMapping("/merge")
    public String merge(@RequestParam String uploadId,
                        @RequestParam int totalChunks,
                        @RequestParam String hdfsPath,
                        @RequestParam String md5) throws Exception {

        if (!hdfsPath.startsWith("/")) {
            hdfsPath = "/" + hdfsPath;
        }

        // 1. 秒传判断
        String exists = hdfs.getPathByMd5(md5);
        if (exists != null && !exists.isEmpty()) {
            hdfs.cleanLocal(uploadId);
            return exists;
        }

        // 2. 本地路径
        File dir = new File(System.getProperty("java.io.tmpdir") + "/upload/" + uploadId);
        if (!dir.exists()) throw new RuntimeException("本地分片目录不存在！");

        File merged = File.createTempFile("merge_" + uploadId + "_", ".tmp");

        System.out.println("开始本地合并: " + merged.getAbsolutePath());

        // 本地合并
        try (FileOutputStream fos = new FileOutputStream(merged)) {

            byte[] buffer = new byte[8 * 1024 * 1024]; // 8MB buffer

            for (int i = 0; i < totalChunks; i++) {

                File part = new File(dir, "chunk-" + i);

                if (!part.exists()) {
                    throw new RuntimeException("缺少分片 chunk-" + i);
                }
                if (part.length() <= 0) {
                    throw new RuntimeException("分片 chunk-" + i + " 空文件！");
                }

                System.out.println("合并 chunk-" + i);

                try (FileInputStream fis = new FileInputStream(part)) {
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        }

        System.out.println("本地合并完毕，准备上传到 HDFS：" + hdfsPath);

        // 3. 上传到 HDFS（一次性）
        String finalUri = hdfs.uploadLocalFile(merged, hdfsPath);

        // 记录秒传表
        hdfs.saveIndex(md5, finalUri);

        // 4. 清理临时文件
        hdfs.cleanLocal(uploadId);
        merged.delete();

        return finalUri;
    }

//    @CrossOrigin
//    @PostMapping("/chunk")
//    public String chunk(@RequestParam("file") MultipartFile file,
//                        @RequestParam String uploadId,
//                        @RequestParam int chunkIndex) throws Exception {
//
//        if (file == null) {
//            System.err.println("ERROR: MultipartFile is null！");
//            return "file-null";
//        }
//
//        if (file.isEmpty()) {
//            System.err.println("ERROR: chunk = " + chunkIndex + " is empty, size=0");
//            return "empty-skip";
//        }
//
//        System.out.println("收到 chunkIndex=" + chunkIndex + " 大小=" + file.getSize());
//
//        try {
//            hdfs.uploadChunk(uploadId, chunkIndex, file.getInputStream());
//            return "ok";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "err: " + e.getMessage();
//        }
//    }

//    @CrossOrigin
//    @PostMapping("/merge")
//    public String merge(@RequestParam String uploadId,
//                        @RequestParam int totalChunks,
//                        @RequestParam String hdfsPath,
//                        @RequestParam String md5) throws Exception {
//
//        if (!hdfsPath.startsWith("/")) hdfsPath = "/" + hdfsPath;
//
//        String exists = hdfs.getPathByMd5(md5);
//        if (exists != null && !exists.isEmpty()) {
//            hdfs.cleanUpload(uploadId);
//            return exists;
//        }
//
//        String finalUri = hdfs.mergeByAppend(uploadId, totalChunks, hdfsPath);
//
//        hdfs.saveIndex(md5, finalUri);
//
//        return finalUri;
//    }




    @CrossOrigin
    @GetMapping("/download")
    public void download(@RequestParam String path, HttpServletResponse resp) throws Exception {

        String filename = new File(new URI(path).getPath()).getName();
        String encoded = java.net.URLEncoder.encode(filename, "UTF-8")
                .replaceAll("\\+", "%20");   // 修复 "+" 转义问题

        resp.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded
        );

        resp.setContentType("application/octet-stream");

        OutputStream out = resp.getOutputStream();
        hdfs.download(path, out);
        out.close();
    }
}
