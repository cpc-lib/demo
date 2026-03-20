package cc.ivera.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cc.ivera.model.dto.UserDTO;
import cc.ivera.model.pojo.User;
import cc.ivera.model.vo.AjaxResult;
import cc.ivera.util.FastJsonUtil;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hutool")
public class HttpUtilController {

    @Value("${amap.key}")
    private String amapKey = "659f057c4e1b3c72ae58027363b3afb9";

    @GetMapping(value = "/get")
    public AjaxResult testGet(@RequestParam("city_code") String cityCode) {
        String url = "https://restapi.amap.com/v3/weather/weatherInfo?extensions=all&key="
                + amapKey + "&city=" + cityCode;
        String json = HttpUtil.get(url);
        Map map = FastJsonUtil.parseJsonToObj(json, Map.class);
        return AjaxResult.success(map.get("forecasts"));
    }


    @GetMapping("/citycode/list")
    public AjaxResult citycodeList() {
        List<Map> datas = new ArrayList<>();

        Map data0 = new HashMap();
        data0.put("cityCode", "440100");
        data0.put("cityName", "广州");

        Map data1 = new HashMap();
        data1.put("cityCode", "310100");
        data1.put("cityName", "上海");

        Map data2 = new HashMap();
        data2.put("cityCode", "430400");
        data2.put("cityName", "衡阳");

        Map data3 = new HashMap();
        data3.put("cityCode", "320100");
        data3.put("cityName", "南京");

        datas.add(data0);
        datas.add(data1);
        datas.add(data2);

        return AjaxResult.success(datas);
    }


    @PostMapping("/post")
    public AjaxResult testPost(UserDTO user) throws JAXBException {
        String body = FastJsonUtil.parseObjToJson(user);
        String post = HttpUtil.post("https://httpbin.org/post", body);
        Map map = FastJsonUtil.parseJsonToObj(post, Map.class);
        String jsonStr = MapUtil.getStr(map, "data");
        UserDTO userDTO = FastJsonUtil.parseJsonToObj(jsonStr, UserDTO.class);
        return AjaxResult.success(userDTO);
    }


    @PostMapping("/post/form")
    public AjaxResult testPostUri() {
        //请求地址
        String url = "https://httpbin.org/post";
        //提交参数设置
        Map map = new HashMap();
        map.put("name", "zhangsan");
        String post = HttpUtil.post(url, map);
        System.out.println(post);
        //System.out.println(s);
        return AjaxResult.success();
    }


    @PostMapping("/post/json")
    public AjaxResult testPosJson() {
        String url = "http://localhost:8080/test/post/v1";
        Map map = new HashMap();
        map.put("name", "zhangsan");
        String body = FastJsonUtil.parseObjToJson(map);
        String post = HttpUtil.post(url, body, 3000);
        System.out.println(post);
        return AjaxResult.success(map);
    }


    @PostMapping("/post/form-data")
    public AjaxResult checkToken(@RequestPart("file") MultipartFile file, User user) {

        String url = "http://localhost:8080/test/filewithArgs/v1";


        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36";

        Map map = new HashMap();

        map.put("id", user.getId());
        map.put("age", user.getAge());
        map.put("name", user.getName());
        map.put("gender", user.getGender());

        //重点 临时文件的使用
        File tempFile = null;

        try {
            String originalFilename = file.getOriginalFilename();
            int i = originalFilename.lastIndexOf(".");
            String extension = originalFilename.substring(i, originalFilename.length());
            tempFile = File.createTempFile("temp", extension);
            file.transferTo(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


        String json = HttpRequest.post(url).header(Header.USER_AGENT, ua)
                .header(Header.ACCEPT,MediaType.APPLICATION_JSON.toString())
                .header(Header.CONTENT_TYPE, String.valueOf(MediaType.MULTIPART_FORM_DATA))
                .form(map)
                .form("file",tempFile)
                .timeout(3000)
                .execute().body();


        System.out.println(json);
        return AjaxResult.success();

    }
}
