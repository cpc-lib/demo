package cn.itcast.nio.c5;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Future;

import static cn.itcast.nio.c2.ByteBufferUtil.debugAll;

@Slf4j
public class AioFileChannel {
    //https://blog.csdn.net/qq_21125183/article/details/90380637 异步读写文件
    public static void main(String[] args) throws IOException {
        //aysncRead();
        //多线程下写不推荐
        for (Integer i = 0; i < 100; i++) {
            Integer number = i;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //synchronized (this){
                        asyncWrite(number.toString());
                        //}
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.start();
        }
    }

    private static void asyncWrite(String data) throws IOException {
        Path path = Paths.get("test.txt");
        //文件需要存在才不会报错
        AsynchronousFileChannel fileChannel =
                AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        long position = 0;
        buffer.put(data.getBytes());
        buffer.flip();
        Future<Integer> operation = fileChannel.write(buffer, position);
        buffer.clear();
        while (!operation.isDone()) ;
        System.out.println("Write done");
    }


    private static void aysncRead() throws IOException {
        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get("data.txt"), StandardOpenOption.READ)) {
            // 参数1 ByteBuffer
            // 参数2 读取的起始位置
            // 参数3 附件
            // 参数4 回调对象 CompletionHandler
            ByteBuffer buffer = ByteBuffer.allocate(16);
            log.debug("read begin...");
            channel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override // read 成功
                public void completed(Integer result, ByteBuffer attachment) {
                    log.debug("read completed...{}", result);
                    attachment.flip();
                    debugAll(attachment);
                }

                @Override // read 失败
                public void failed(Throwable exc, ByteBuffer attachment) {
                    exc.printStackTrace();
                }
            });
            log.debug("read end...");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.in.read();
    }
}
