
package cc.ivera.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

@Service
public class ChunkUploadService {

    @Value("${upload.temp-dir}")
    private String dir;

    public boolean check(String uid,int idx){
        return Files.exists(Paths.get(dir,uid,idx+".part"));
    }

    public void save(InputStream in,String uid,int idx) throws IOException {
        Path d=Paths.get(dir,uid);
        Files.createDirectories(d);
        Path p=d.resolve(idx+"");
        OutputStream out=Files.newOutputStream(p);
        byte[] buf=new byte[1024*1024];
        int len;
        while((len=in.read(buf))!=-1) out.write(buf,0,len);
        out.close();
        in.close();
    }

    public File merge(String uid,int total) throws IOException {
        Path d=Paths.get(dir,uid);
        Path m=d.resolve("merged.tmp");
        OutputStream out=Files.newOutputStream(m);
        byte[] buf=new byte[1024*1024];
        for(int i=0;i<total;i++){
            Path p=d.resolve(i+"");
            InputStream in=Files.newInputStream(p);
            int len;
            while((len=in.read(buf))!=-1) out.write(buf,0,len);
            in.close();
        }
        out.close();
        return m.toFile();
    }

    public void clean(String uid) throws IOException {
        Path d=Paths.get(dir,uid);
        if(!Files.exists(d)) return;
        Files.walk(d).sorted((a,b)->b.compareTo(a))
            .forEach(p->{
                try { Files.deleteIfExists(p); }
                catch(Exception e){}
            });
    }
}
