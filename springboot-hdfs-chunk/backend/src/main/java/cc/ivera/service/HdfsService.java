package cc.ivera.service;

import org.apache.hadoop.fs.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;

@Service
public class HdfsService {

    private final FileSystem fs;
    private final FileIndexService indexService;

    public HdfsService(FileSystem fs, FileIndexService indexService) {
        this.fs = fs;
        this.indexService = indexService;
    }

    private String baseDir(String uploadId) { return "/tmp/upload/" + uploadId; }

    private String chunkPath(String uploadId, int index) {
        return baseDir(uploadId) + "/chunk-" + index;
    }

    /**
     * 分片上传：写入临时目录
     */
    public void uploadChunk(String uploadId, int index, InputStream in) throws IOException {
        String p = chunkPath(uploadId, index);
        Path path = new Path(p);
        Path parent = path.getParent();
        if (!fs.exists(parent)) fs.mkdirs(parent);

        FSDataOutputStream out = fs.create(path, true, 1024 * 1024);
        byte[] buf = new byte[1024 * 1024];
        int len;
        try {
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.hflush();
            out.hsync();
        } finally {
            out.close();
            in.close();
        }

        // 删除空块
        if (fs.getFileStatus(path).getLen() == 0) {
            fs.delete(path, false);
        }
    }


    /**
     * append 顺序无损合并
     */
    public String mergeByAppend(String uploadId, int totalChunks, String finalPath) throws IOException {

        if (!finalPath.startsWith("/")) finalPath = "/" + finalPath;

        Path finalP = new Path(finalPath);
        Path parent = finalP.getParent();
        if (!fs.exists(parent)) fs.mkdirs(parent);

        if (fs.exists(finalP)) fs.delete(finalP, false);
        fs.create(finalP).close();

        byte[] buf = new byte[1024 * 1024];

        for (int i = 0; i < totalChunks; i++) {
            Path chunk = new Path(chunkPath(uploadId, i));

            if (!fs.exists(chunk))
                throw new IOException("missing chunk: " + chunk);

            FileStatus st = fs.getFileStatus(chunk);
            if (st.getLen() <= 0)
                throw new IOException("empty chunk: " + chunk);

            FSDataInputStream in = fs.open(chunk);
            FSDataOutputStream out = fs.append(finalP);

            int len;
            try {
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.hflush();
                out.hsync();
            } finally {
                out.close();
                in.close();
            }
        }

        // 删除 temp
        fs.delete(new Path(baseDir(uploadId)), true);

        return finalP.toUri().toString(); // 返回绝对 HDFS URI
    }


    // HdfsService.java 里增加这个方法
    public String uploadLocalFile(File localFile, String hdfsPath) throws IOException {

        // 确保是绝对路径
        if (!hdfsPath.startsWith("/")) {
            hdfsPath = "/" + hdfsPath;
        }

        Path target = new Path(hdfsPath);

        // 确保目录存在
        Path parent = target.getParent();
        if (parent != null && !fs.exists(parent)) {
            fs.mkdirs(parent);
        }

        // 已存在则删除
        if (fs.exists(target)) {
            fs.delete(target, false);
        }

        // 从本地文件流复制到 HDFS
        FileInputStream fis = null;
        FSDataOutputStream out = null;
        try {
            fis = new FileInputStream(localFile);
            out = fs.create(target, true);

            byte[] buffer = new byte[8 * 1024 * 1024]; // 8MB 提升速度
            int len;
            while ((len = fis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.hflush();
            out.hsync();
        } finally {
            if (out != null) {
                out.close();
            }
            if (fis != null) {
                fis.close();
            }
        }

        // 返回 HDFS 绝对 URI，例如：hdfs://192.168.220.102:8020/upload/xxx.mp4
        return target.toUri().toString();
    }


    public boolean chunkExists(String uploadId, int index) throws IOException {
        return fs.exists(new Path(chunkPath(uploadId, index)));
    }

    /**
     * 清理本地分片临时目录
     * Windows / Linux 全兼容
     */
    public void cleanLocal(String uploadId) {
        // 系统临时目录：Windows = C:\Users\xxx\AppData\Local\Temp\
        //              Linux   = /tmp/
        String base = System.getProperty("java.io.tmpdir") + File.separator + "upload" + File.separator + uploadId;

        File dir = new File(base);

        System.out.println("清理本地分片目录: " + dir.getAbsolutePath());

        if (!dir.exists()) {
            return;
        }

        // 先删文件，再删目录
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    System.out.println("删除: " + f.getAbsolutePath());
                    f.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 删除分片目录
        try {
            dir.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void download(String hdfsUri, OutputStream out) throws IOException {
        FSDataInputStream in = fs.open(new Path(URI.create(hdfsUri)));

        byte[] buf = new byte[1024 * 1024];
        int len;
        try {
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        } finally {
            in.close();
            out.flush();
        }
    }

    public String getPathByMd5(String md5) { return indexService.getPathByMd5(md5); }

    public void saveIndex(String md5, String path) { indexService.save(md5, path); }

    public void cleanUpload(String uploadId) throws IOException {
        fs.delete(new Path(baseDir(uploadId)), true);
    }
}
