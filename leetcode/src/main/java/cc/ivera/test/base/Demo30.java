package cc.ivera.test.base;

import cn.hutool.core.map.MapUtil;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import cc.ivera.util.AmpUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class Demo30 {
    public static void main(String[] args) throws Exception {
        //智能手机拍摄的图片一般不会带有gps信息，
        //此处使用magicExif软件添加gps信息 下载与使用地址
        //下载地址 https://www.52pojie.cn/thread-1027678-1-1.html
        //经纬度转度分秒工具 https://www.bejson.com/other/jwd/
        //坐标获取工具 https://api.map.baidu.com/lbsapi/getpoint/index.html
        //采用高德地图的第三方接口获取地址信息。
        Resource resource = new ClassPathResource("demo1.jpg");
        File file = resource.getFile();
        String path = file.getAbsolutePath();
        HashMap<String, Object> map = readPicInfo(path);
        String latitude = MapUtil.getStr(map, "latitude");
        String longitude = MapUtil.getStr(map, "longitude");
        Double[] point = convert(latitude, longitude);
        System.out.println(point[1] + "," + point[0]);
        String result = AmpUtil.getAMapByLngAndLat(point[1].toString(), point[0].toString());
        Document document = Jsoup.parse(result);
        Elements elementsByTag = document.getElementsByTag("formatted_address");
        String address = elementsByTag.get(0).text();
        System.out.println(address);

    }

    public static HashMap<String, Object> readPicInfo(String file_path) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Tag tag = null;
        File jpegFile = new File(file_path);
        Metadata metadata;
        try {
            metadata = JpegMetadataReader.readMetadata(jpegFile);
            Iterator<Directory> it = metadata.getDirectories().iterator();
            while (it.hasNext()) {
                Directory exif = it.next();
                Iterator<Tag> tags = exif.getTags().iterator();
                while (tags.hasNext()) {
                    tag = (Tag) tags.next();
                    if (tag.getTagName().equals("GPS Latitude")) {
                        map.put("latitude", tag.getDescription());
                    }
                    if (tag.getTagName().equals("GPS Longitude")) {
                        map.put("longitude", tag.getDescription());
                    }
                }
            }
        } catch (JpegProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 经纬度转换
     *
     * @param latitude
     * @param longitude
     */
    public static Double[] convert(String latitude, String longitude) {


        double a = Double.parseDouble(latitude.split(" ")[0].replace("°", ""));
        double b = Double.parseDouble(latitude.split(" ")[1].replace("'", "")) / 60;
        double c = Double.parseDouble(latitude.split(" ")[2].replace("\"", "")) / 60 / 60;
        Double[] address = new Double[2];
        address[0] = a + b + c;
        double d = Double.parseDouble(longitude.split(" ")[0].replace("°", ""));
        double e = Double.parseDouble(longitude.split(" ")[1].replace("'", "")) / 60;
        double f = Double.parseDouble(longitude.split(" ")[2].replace("\"", "")) / 60 / 60;
        address[1] = d + e + f;
        return address;
    }

}
