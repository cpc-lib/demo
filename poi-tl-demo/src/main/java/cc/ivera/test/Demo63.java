package cc.ivera.test;

import cc.ivera.policy.MergeRowsRenderPolicy;
import cc.ivera.policy.MultiImageRenderPolicy;
import cn.hutool.core.io.resource.ClassPathResource;
import com.alibaba.fastjson2.JSONObject;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.util.PoitlIOUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Demo63 {

    /**
     * 浏览器导出
     *
     * @param fileName  文件名称
     * @param map       数据集合
     * @param configure 配置信息
     */
    public static void exportFile(String fileName, Map<String, Object> map, Configure configure) {
        try {

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletResponse response = attributes.getResponse();

            // 获取Word模板，模板存放路径在项目的resources目录下
            //PoiController就是方法所在的类
            XWPFTemplate template = XWPFTemplate.compile(fileName, configure).render(map);

            //处理文件名乱码
            String attachName = new String(("测试.docx").getBytes(), "ISO-8859-1");

            // 浏览器端下载
            response.setContentType("application/x-download;charset=" + "utf-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + attachName);

            OutputStream out = response.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(out);
            template.write(bos);
            bos.flush();
            out.flush();
            PoitlIOUtils.closeQuietlyMulti(template, bos, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // 获取模板文件流
        ClassPathResource classPathResource = new ClassPathResource("poi-tl/test4.docx");
        File file = classPathResource.getFile();
        String httpPicturePaths = "http://deepoove.com/images/icecream.png,http://deepoove.com/images/icecream.png";

        //单个图片数据模板
//        PictureRenderData picture = Pictures.ofStream(new FileInputStream(picturePath), PictureType.JPEG)
//                .size(400, 300).create();


        //多张图片处理
//        List<Map<String, Object>> pictureList = new ArrayList(){{
//            for (int i = 0; i < 2; i++) {
//                add(new HashMap<String,Object>(){{
//                    put("url", Pictures.ofStream(new FileInputStream(picturePath), PictureType.JPEG)
//                            .size(220,150).create());
//                }});
//            }}};

        //如果表格内不需要合并数据直接put("",new ArrayList<string,object>/new ArrayList<实体类>)

        //单元格多图片
        //ArrayList<JSONObject> arrayList = getArrayList(picturePaths,httpPicturePaths);
        ArrayList<JSONObject> arrayList = getArrayList("", httpPicturePaths);

        // 伪造一个表格数据
        //具体要合并的列
//        List<Integer> mergeColumnList = Arrays.asList(0,3);
//        ServerTableData oneTable = getServerTableData(mergeColumnList);

        //表格绑定
        Configure config = Configure.builder()
                //注册添加前缀为'&'的自定义插件：
                .addPlugin('&', new MultiImageRenderPolicy())
                //含图片数据表格合并同列相邻的单元格
                .bind("items", new MergeRowsRenderPolicy())
                .build();

        XWPFTemplate template = XWPFTemplate.compile(file, config).render(
                new HashMap<String, Object>() {{
                    //单元格多个图片
                    put("items", arrayList);
                }});

        //输出网络流 本地导出
        template.writeAndClose(new FileOutputStream("output4.docx"));

        //网络导出
        //exportFile(filePath,hashMap,config);
    }

    /**
     *  表格合并数据处理
     * @param mergeColumnList 要合并的列集合
     * @return 数据集合
     */
//    private static ServerTableData getServerTableData(List<Integer> mergeColumnList) {
//        ServerTableData serverTableData = new ServerTableData();
//        List<RowRenderData> serverDataList = new ArrayList<>();
//        for (int j = 0; j < 5; j++) {
//            String typeName;
//            if (j > 1) {
//                typeName = "张三";
//                serverDataList.add(Rows.of(typeName, "男", "3", "喝酒").center().create());
//            }else {
//                typeName = "李四";
//                serverDataList.add(Rows.of(typeName, "女", "4", "逛街").center().create());
//            }
//        }
//
//        List<Map<String, Object>> groupDataList = new ArrayList<>();
//        Map<String, Object> groupData1 = new HashMap<>();
//        //typeName是张三的数据有两条
//        groupData1.put("typeName", "张三");
//        groupData1.put("listSize", "3");
//        Map<String, Object> groupData2 = new HashMap<>();
//        //typeName是李四的数据有三条
//        groupData2.put("typeName", "李四");
//        groupData2.put("listSize", "2");
//        groupDataList.add(groupData1);
//        groupDataList.add(groupData2);
//
//        //具体合并的列
//        serverTableData.setMergeColumnList(mergeColumnList);
//        //表格中数据
//        serverTableData.setServerDataList(serverDataList);
//        //分组的信息
//        serverTableData.setGroupDataList(groupDataList);
//        //从哪列开始合并
//        serverTableData.setMergeColumn(0);
//        return serverTableData;
//    }

    /**
     * 含图片表格数据处理, 可单独抽成工具类,官网支持多种不同图片处理
     *
     * @param picturePaths     java图片路径
     * @param httpPicturePaths 网络图片路径
     * @return 数据集合
     * @throws IOException 抛出io流异常
     */
    public static ArrayList<JSONObject> getArrayList(String picturePaths, String httpPicturePaths) throws IOException, IOException {

        //String[] picturePathArr = picturePaths.split(",");
        String[] httpPicturePathArr = httpPicturePaths.split(",");

        //word模板中使用 [&images]来插入多张图片了
        ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>() {{
            //网络图片
            add(new JSONObject().fluentPut("title", "第1组图片")
                    .fluentPut("detail", "这是第1组图片描述")
                    .fluentPut("images", new ArrayList<PictureRenderData>() {{
                        for (String httpPicturePath : httpPicturePathArr) {
                            add(Pictures.ofUrl(httpPicturePath).size(50,50).create());
                        }
                    }}));
            //网络图片
            add(new JSONObject().fluentPut("title", "第2组图片")
                    .fluentPut("detail", "这是第2组图片描述")
                    .fluentPut("images", new ArrayList<PictureRenderData>() {{
                        for (String httpPicturePath : httpPicturePathArr) {
                            add(Pictures.ofUrl(httpPicturePath).size(50,50).create());
                        }
                    }}));

            // java图片
//            add(new JSONObject().fluentPut("title", "第3组图片")
//                    .fluentPut("detail", "这是第3组图片描述")
//                    .fluentPut("images", new ArrayList<PictureRenderData>() {{
//                        for (String picturePath : picturePathArr) {
//                            BufferedImage bufferedImage = ImageIO.read(new FileInputStream(picturePath));
//                            add(Pictures.ofBufferedImage(bufferedImage, PictureType.PNG)
//                                    .size(205, 205).create());
//                        }
//                    }}));
        }};
        return arrayList;
    }

}