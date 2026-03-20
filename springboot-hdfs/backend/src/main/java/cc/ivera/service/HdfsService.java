
package cc.ivera.service;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Service
public class HdfsService {
    private final FileSystem fs;

    public HdfsService(FileSystem fs){
        this.fs=fs;
    }

    public void upload(InputStream in,String p) throws IOException {
        FSDataOutputStream out = fs.create(new Path(p), true);
        byte[] buf=new byte[1024*1024];
        int len;
        while((len=in.read(buf))!=-1) out.write(buf,0,len);
        out.close();
        in.close();
    }

    public void download(String p, HttpServletResponse resp) throws IOException {
        Path path = new Path(p);
        if(!fs.exists(path)){
            resp.setStatus(404);
            resp.getWriter().write("Not Found");
            return;
        }
        FSDataInputStream in = fs.open(path);
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition","attachment; filename="+path.getName());
        OutputStream out = resp.getOutputStream();
        byte[] buf=new byte[4096];
        int len;
        while((len=in.read(buf))!=-1) out.write(buf,0,len);
        out.flush();
        in.close();
    }
}
