package cc.ivera.util;


import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

@RunWith(Parameterized.class)
@SpringBootTest
public class VideoUtils implements MultipartFile {

    private final byte[] imgContent;
    private final String header;

    public VideoUtils(byte[] imgContent, String header) {
        this.imgContent = imgContent;
        this.header = header;
    }

    /**
     * 例子
     *
     * @param args
     */
    @Test
    public static void main(String[] args) throws Exception {
        String s = fetchFrame("https://dev-vedio-1303824005.cos.ap-beijing.myqcloud.com/video/51346c41-0ca0-4e74-9d50-b5393ba55535.mp4");
        System.out.println(s);
        System.out.println(base64ToMultipart(s));
    }

    public static String fetchFrame(String videoPath) throws Exception {
        FFmpegFrameGrabber ff = null;
        byte[] data = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        File file = new File("D:\\media\\image\\test.jpg");
        try {
            ff = new FFmpegFrameGrabber(videoPath);
            ff.start();
            int lenght = ff.getLengthInFrames();
            int i = 0;
            Frame f = null;
            while (i < lenght) {
                // 过滤前5帧，避免出现全黑的图片
                f = ff.grabFrame();
                if ((i > 5) && (f.image != null)) {
                    break;
                }
                i++;
            }
            BufferedImage bi = new Java2DFrameConverter().getBufferedImage(f);
            String rotate = ff.getVideoMetadata("rotate");
            if (rotate != null) {
                bi = rotate(bi, Integer.parseInt(rotate));
            }
            //TODO 将文件传到oss服务器保存到数据库的记录中
            ImageIO.write(bi, "jpg", os);
            ImageIO.write(bi, "jpg", file);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ff != null) {
                    ff.stop();
                }
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }

        return "data:image/jpg;base64," + Base64Util.encode(os.toByteArray());
    }

    public static BufferedImage rotate(BufferedImage src, int angel) {
        int src_width = src.getWidth(null);
        int src_height = src.getHeight(null);
        int type = src.getColorModel().getTransparency();
        Rectangle rect_des = calcRotatedSize(new Rectangle(new Dimension(src_width, src_height)), angel);
        BufferedImage bi = new BufferedImage(rect_des.width, rect_des.height, type);
        Graphics2D g2 = bi.createGraphics();
        g2.translate((rect_des.width - src_width) / 2, (rect_des.height - src_height) / 2);
        g2.rotate(Math.toRadians(angel), src_width / 2, src_height / 2);
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return bi;
    }

    public static Rectangle calcRotatedSize(Rectangle src, int angel) {
        if (angel >= 90) {
            if (angel / 90 % 2 == 1) {
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angel = angel % 90;
        }
        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
        double angel_alpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angel_dalta_width = Math.atan((double) src.height / src.width);
        double angel_dalta_height = Math.atan((double) src.width / src.height);
        int len_dalta_width = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_width));
        int len_dalta_height = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_height));
        int des_width = src.width + len_dalta_width * 2;
        int des_height = src.height + len_dalta_height * 2;
        return new Rectangle(new Dimension(des_width, des_height));
    }

    /**
     * 将base64转换成MultipartFile
     *
     * @param base64
     * @return
     */
    public static MultipartFile base64ToMultipart(String base64) {
        try {
            String[] baseStrs = base64.split(",");

            byte[] b = new byte[0];
            b = Base64Util.decode(baseStrs[1]);

            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
            return new VideoUtils(b, baseStrs[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String getName() {
        // TODO - implementation depends on your requirements
        return System.currentTimeMillis() + Math.random() + "." + header.split("/")[1];
    }

    @Override
    public String getOriginalFilename() {
        // TODO - implementation depends on your requirements
        return System.currentTimeMillis() + (int) Math.random() * 10000 + "." + header.split("/")[1];
    }

    @Override
    public String getContentType() {
        // TODO - implementation depends on your requirements
        return header.split(":")[1];
    }

    @Override
    public boolean isEmpty() {
        return imgContent == null || imgContent.length == 0;
    }

    @Override
    public long getSize() {
        return imgContent.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return imgContent;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(imgContent);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        new FileOutputStream(dest).write(imgContent);
    }

}
