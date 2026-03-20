package cc.ivera.test.base;

import cn.hutool.core.io.resource.ClassPathResource;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import cc.ivera.model.pojo.PoiUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Demo62 {


    //1.测试本地文件查询图片
    @Test
    public void test1() throws IOException {
        //1.从resource中读取文件
        ClassPathResource classPathResource = new ClassPathResource("poi-tl/test1.docx");
        File file = classPathResource.getFile();
        ClassPathResource class2 = new ClassPathResource("test.png");
        String absolutePath = class2.getFile().getAbsolutePath();
        Map<String, Object> map = new HashMap<>();
        map.put("contract_name", "测试合同");
        map.put("web_image", Pictures.ofUrl("http://deepoove.com/images/icecream.png").size(100, 100).create());
        map.put("local_image", Pictures.ofLocal(absolutePath).size(255, 500).create());
        XWPFTemplate template = XWPFTemplate.compile(file).render(map);
        template.writeAndClose(new FileOutputStream("output1.docx"));
    }


    /**
     * 2.列表导出
     *
     * @throws IOException
     */
    @Test
    public void test2() throws IOException {
        //1.从resource中读取文件
        ClassPathResource classPathResource = new ClassPathResource("poi-tl/test2.docx");
        File file = classPathResource.getFile();
        LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();

        Configure config = Configure.builder()
                .bind("users", policy).build();

        //缺点图片没有压缩
        List<PoiUser> users = List.of(
                new PoiUser("张三", 18, Pictures.ofUrl("http://deepoove.com/images/icecream.png").size(50, 50).create(), new BigDecimal(3000)),
                new PoiUser("李四", 19, Pictures.ofUrl("http://deepoove.com/images/icecream.png").size(50, 50).create(), new BigDecimal(3000)),
                new PoiUser("王五", 20, Pictures.ofUrl("http://deepoove.com/images/icecream.png").size(50, 50).create(), new BigDecimal(3000))
        );

        XWPFTemplate template = XWPFTemplate.compile(file, config).render(
                new HashMap<String, Object>() {{
                    put("users", users);
                }}
        );

        template.writeAndClose(new FileOutputStream("output2.docx"));
    }


    /**
     * 3.多表头 - 员工工资条
     */
    @Test
    public void test3() throws IOException {
        //1.从resource中读取文件
        ClassPathResource classPathResource = new ClassPathResource("poi-tl/test3.docx");
        File file = classPathResource.getFile();
        LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();

        Configure config = Configure.builder()
                .bind("users", policy).build();

        //图片压缩
        List<PoiUser> users = List.of(
                new PoiUser("张三", 18, Pictures.ofUrl("http://deepoove.com/images/icecream.png").size(50, 50).create(), new BigDecimal(3000)),
                new PoiUser("李四", 19, Pictures.ofUrl("http://deepoove.com/images/icecream.png").size(50, 50).create(), new BigDecimal(3000)),
                new PoiUser("王五", 20, Pictures.ofUrl("http://deepoove.com/images/icecream.png").size(50, 50).create(), new BigDecimal(3000))
        );

        Map<String, Object> retMap = new HashMap<>();

        List<Map<String, Object>> list = new ArrayList<>();
        for (PoiUser user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("profile", user.getProfile());
            map.put("salary", user.getSalary());
            list.add(map);
        }

        retMap.put("users", users);

        XWPFTemplate template = XWPFTemplate.compile(file, config).render(retMap);

        template.writeAndClose(new FileOutputStream("output3.docx"));
    }


}
