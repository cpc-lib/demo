package cc.ivera.util;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NioDownloadUtil {

    /**
     * @param file: 本地文件
     */
    public static void downloadDoc(File file, HttpServletResponse response) throws IOException, IOException {
        OutputStream outputStream = response.getOutputStream();
        String contentType = Files.probeContentType(Paths.get(file.getAbsolutePath()));
        //设置响应头
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Disposition", "attachment;filename=" + new String(file.getName().getBytes("utf-8"), "ISO8859-1"));
        response.setContentLength((int) file.length());
        //获取文件输入流
        FileInputStream fileInputStream = new FileInputStream(file);
        //获取输出流通道
        WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);
        FileChannel fileChannel = fileInputStream.getChannel();
        //采用零拷贝的方式实现文件的下载
        fileChannel.transferTo(0, fileChannel.size(), writableByteChannel);
        //关闭对应的资源
        fileChannel.close();
        outputStream.flush();
        writableByteChannel.close();
    }

    public static void downloadDoc(String path, HttpServletResponse response) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在");
        }
        downloadDoc(file, response);
    }

}
