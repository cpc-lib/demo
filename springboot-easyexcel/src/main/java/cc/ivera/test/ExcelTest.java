package cc.ivera.test;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.WriteDirectionEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import cc.ivera.domain.*;
import cc.ivera.service.UserService;

import javax.annotation.Resource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ExcelTest {

    @Resource(name="userService")
    private UserService userService;

    @Test
    public void testWriteExcel1() {

        String filename = "E:\\home\\easyexcel\\user1.xlsx";

        // 向Excel中写入数据 也可以通过 head(Class<?>) 指定数据模板
        EasyExcel.write(filename, User.class)
                .sheet("用户信息")
                .doWrite(userService.getUserList());
    }

    @Test
    public void testWriteExcel2() {
        String filename = "E:\\home\\easyexcel\\user2.xlsx";
        // 创建ExcelWriter对象
        ExcelWriter excelWriter = EasyExcel.write(filename, User.class).build();
        // 创建Sheet对象
        WriteSheet writeSheet = EasyExcel.writerSheet("用户信息").build();
        // 向Excel中写入数据
        excelWriter.write(userService.getUserList(), writeSheet);
        // 关闭流
        excelWriter.finish();
    }

    @Test
    public void testWriteExcel3() {
        String filename = "E:\\home\\easyexcel\\user3.xlsx";
        // 设置排除的属性 也可以在数据模型的字段上加@ExcelIgnore注解排除
        Set<String> excludeFields = new HashSet<>();
        excludeFields.add("rank");
        excludeFields.add("createtime");
        // 写Excel
        EasyExcel.write(filename, User.class)
                .excludeColumnFiledNames(excludeFields)
                .sheet("用户信息")
                .doWrite(userService.getUserList());
    }


    @Test
    public void testWriteExcel4() {
        String filename = "E:\\home\\easyexcel\\user4.xlsx";
        // 设置要导出的字段
        Set<String> includeFields = new HashSet<>();
        includeFields.add("rank");
        includeFields.add("createtime");
        // 写Excel
        EasyExcel.write(filename, User.class)
                .includeColumnFiledNames(includeFields)
                .sheet("用户信息")
                .doWrite(userService.getUserList());
    }


    /**
     * 使用index属性指定列的顺序
     */
    @Test
    public void testWriteExcel5() {
        String filename = "E:\\home\\easyexcel\\user5.xlsx";
        // 向Excel中写入数据
        EasyExcel.write(filename, User.class)
                .sheet("用户信息")
                .doWrite(userService.getUserList());
    }


    @Test
    public void testWriteExcel6() {
        String filename = "E:\\home\\easyexcel\\user6.xlsx";
        List<ComplexHeadUser> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ComplexHeadUser user = ComplexHeadUser.builder()
                    .userId(i)
                    .userName("大哥" + i)
                    .hireDate(new Date())
                    .build();
            users.add(user);
        }
        // 向Excel中写入数据
        EasyExcel.write(filename, ComplexHeadUser.class)
                .sheet("用户信息")
                .doWrite(users);
    }


    /**
     * 写入数据到不同的
     */
    @Test
    public void testWriteExcel7() {
        String filename = "E:\\home\\easyexcel\\user7.xlsx";
        // 创建ExcelWriter对象
        ExcelWriter excelWriter = EasyExcel.write(filename, User.class).build();
        // 向Excel的不同Sheet写入相同数据
        for (int i = 0; i < 2; i++) {
            // 创建Sheet对象
            WriteSheet writeSheet = EasyExcel.writerSheet("用户信息" + i).build();
            excelWriter.write(userService.getUserList(), writeSheet);
        }
        // 关闭流
        excelWriter.finish();
    }


    /**
     * 日期与金额格式处理
     */
    @Test
    public void testWriteExcel8() {
        String filename = "E:\\home\\easyexcel\\user8.xlsx";
        // 向Excel中写入数据
        EasyExcel.write(filename, UserDemo.class)
                .sheet("用户信息")
                .doWrite(getUserData());
    }


    private static List<UserDemo> getUserData(){
        List<UserDemo> temp = new ArrayList<>();
        UserDemo userDemo = new UserDemo();
        userDemo.setUserId(1);
        userDemo.setUserName("linus");
        userDemo.setGender("1");
        userDemo.setHireDate(new Date());
        userDemo.setSalary(10000.06);
        temp.add(userDemo);
        return temp;

    }


    @Test
    public void testWriteImageToExcel() throws IOException, FileNotFoundException, MalformedURLException {
        String filename = "E:\\home\\easyexcel\\user9.xlsx";
        // 图片位置
        String imagePath = "E:\\home\\bytedance\\wallhaven\\1661688807215.jpg";
        // 网络图片
        URL url = new URL("https://cn.bing.com/th?id=OHR.TanzaniaBeeEater_ZH-CN3246625733_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp");
        // 将图片读取到二进制数据中
        byte[] bytes = new byte[(int) new File(imagePath).length()];
        InputStream inputStream = new FileInputStream(imagePath);
        inputStream.read(bytes, 0, bytes.length);

        List<ImageData> imageDataList = new ArrayList<>();

        // 添加要写入的图片模型
        for(int i=0;i<5;i++){
            // 创建数据模板
            ImageData imageData = ImageData
                    .builder().file(new File(imagePath))
                    //自动关闭
                    .inputStream(new FileInputStream(imagePath))
                    .str(imagePath)
                    .byteArr(bytes)
                    .url(url)
                    .build();
            imageDataList.add(imageData);
        }


        // 写数据
        EasyExcel.write(filename, ImageData.class)
                .sheet("帅哥")
                .doWrite(imageDataList);
    }


    @Test
    public void testWriteExcel9() {
        String filename = "E:\\home\\easyexcel\\user10.xlsx";
        // 构建数据
        List<WidthAndHeightData> dataList = new ArrayList<>();
        WidthAndHeightData data = WidthAndHeightData.builder()
                .string("字符串")
                .date(new Date())
                .doubleData(888.88)
                .build();
        dataList.add(data);
        // 向Excel中写入数据
        EasyExcel.write(filename, WidthAndHeightData.class)
                .sheet("行高和列宽测试")
                .doWrite(dataList);
    }


    @Test
    public void testWrite11() {
        String filename = "E:\\home\\easyexcel\\user11.xlsx";
        // 构建数据
        List<DemoStyleData> dataList = new ArrayList<>();
        DemoStyleData data = DemoStyleData.builder()
                .string("字符串")
                .date(new Date())
                .doubleData(888.88)
                .build();
        dataList.add(data);
        // 向Excel中写入数据
        EasyExcel.write(filename, DemoStyleData.class)
                .sheet("样式设置测试")
                .doWrite(dataList);

    }


    @Test
    public void testWrite12() {
        String filename = "E:\\home\\easyexcel\\user12.xlsx";
        // 构建数据
        List<DemoMergeData> dataList = new ArrayList<>();
        DemoMergeData data = DemoMergeData.builder()
                .string("字符串")
                .date(new Date())
                .doubleData(888.88)
                .build();
        dataList.add(data);
        // 向Excel中写入数据
        EasyExcel.write(filename, DemoMergeData.class)
                .sheet("单元格合并测试")
                .doWrite(dataList);
    }

    @Test
    public void testWriteExcel() {

        String filename = "E:\\home\\easyexcel\\user13.xlsx";

        // 向Excel中写入数据
        EasyExcel.write(filename, UserModel.class)
                .sheet("用户信息")
                .doWrite(getUserData2());
    }

    // 根据user模板构建数据
    private List<UserModel> getUserData2() {
        List<UserModel> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            UserModel user = UserModel.builder()
                    .userId(i)
                    .userName("admin" + i)
                    .gender(i % 2 == 0 ? 0 : 2) // 性别枚举
                    .salary(i * 1000 + 8.888)
                    .hireDate(new Date())
                    .build();
            users.add(user);
        }
        return users;
    }


    /***
     * 在读取Excel表格数据时,
     * 没有指定实体类，
     * 将读取的每行记录映射成一条LinkedHashMap记录。
     */

    @Test
    public void testRead() {
        String filename = "E:\\home\\easyexcel\\read.xlsx";
        // 创建ExcelReaderBuilder对象
        ExcelReaderBuilder readerBuilder = EasyExcel.read();
        // 获取文件对象
        readerBuilder.file(filename);
        // 指定映射的数据模板
//  readerBuilder.head(DemoData.class);
        // 指定sheet
        readerBuilder.sheet(0);
        // 自动关闭输入流
        readerBuilder.autoCloseStream(true);
        // 设置Excel文件格式
        readerBuilder.excelType(ExcelTypeEnum.XLSX);
        // 注册监听器进行数据的解析
        readerBuilder.registerReadListener(new AnalysisEventListener() {
            // 每解析一行数据,该方法会被调用一次
            @Override
            public void invoke(Object demoData, AnalysisContext analysisContext) {
                // 如果没有指定数据模板, 解析的数据会封装成 LinkedHashMap返回
                // demoData instanceof LinkedHashMap 返回 true
                System.out.println("解析数据为:" + demoData.toString());
            }

            // 全部解析完成被调用
            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                System.out.println("解析完成...");
                // 可以将解析的数据保存到数据库
            }
        });
        readerBuilder.doReadAll();
    }


    @Test
    public void testReadExcel() {
        // 读取的excel文件路径
        String filename = "E:\\home\\easyexcel\\read.xlsx";
        // 读取excel
        EasyExcel.read(filename, DemoData.class, new AnalysisEventListener<DemoData>() {
            // 每解析一行数据,该方法会被调用一次
            @Override
            public void invoke(DemoData demoData, AnalysisContext analysisContext) {
                System.out.println("解析数据为:" + demoData.toString());
            }
            // 全部解析完成被调用
            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                System.out.println("解析完成...");
                // 可以将解析的数据保存到数据库
            }
        }).sheet(0).doRead();
    }

    @Test
    public void testReadAllExcelSheet() {
        // 读取的excel文件路径
        String filename = "E:\\home\\easyexcel\\read.xlsx";
        // 读取excel
        EasyExcel.read(filename, DemoData.class, new AnalysisEventListener<DemoData>() {

                    // 每解析一行数据,该方法会被调用一次
                    @Override
                    public void invoke(DemoData demoData, AnalysisContext analysisContext) {
                        System.out.println("解析数据为:" + demoData.toString());
                    }

                    // 解析完一个sheet后被调用
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        System.out.println("解析完成...");
                        // 可以将解析的数据保存到数据库
                    }
                })
//         .sheet(0).doRead();
                .doReadAll(); // 读取全部sheet

    }


    @Test
    public void testReadExcel2() {
        // 读取的excel文件路径
        String filename = "E:\\home\\easyexcel\\read.xlsx";
        // 创建一个数据格式来装读取到的数据
        Class<DemoData> head = DemoData.class;
        // 创建ExcelReader对象
        ExcelReader excelReader = EasyExcel.read(filename, head,
                new AnalysisEventListener<DemoData>() {
                    // 每解析一行数据,该方法会被调用一次
                    @Override
                    public void invoke(DemoData demoData, AnalysisContext analysisContext) {
                        System.out.println("解析数据为:" + demoData.toString());
                    }

                    // 全部解析完成被调用
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        System.out.println("解析完成...");
                        // 可以将解析的数据保存到数据库
                    }
                }).build();
        // 创建sheet对象,并读取Excel的第一个sheet(下标从0开始), 也可以根据sheet名称获取
        ReadSheet sheet = EasyExcel.readSheet(0).build();
        // 读取sheet表格数据 , 参数是可变参数,可以读取多个sheet
//        excelReader.read(sheet);
        excelReader.readAll(); // 读所有sheet
        // 需要自己关闭流操作,在读取文件时会创建临时文件,如果不关闭,会损耗磁盘,严重的磁盘爆掉
        excelReader.finish();
    }

    @Test
    public void testReadExcel3() {
        // 读取的excel文件路径
        String filename = "E:\\home\\easyexcel\\read.xlsx";
        // 构建ExcelReader对象
        ExcelReader excelReader = EasyExcel.read(filename).build();
        // 构建sheet对象
        ReadSheet sheet0 = EasyExcel.readSheet(0)
                .head(DemoData.class) // 指定sheet0的数据模板
                .registerReadListener(new AnalysisEventListener<DemoData>() {
                    // 每解析一行数据,该方法会被调用一次
                    @Override
                    public void invoke(DemoData demoData, AnalysisContext analysisContext) {
                        System.out.println("解析数据1为:" + demoData.toString());
                    }

                    // 全部解析完成被调用
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        System.out.println("解析1完成...");
                        // 可以将解析的数据保存到数据库
                    }
                }).build();

        // 读取sheet,有几个就构建几个sheet进行读取
        excelReader.read(sheet0);

        ReadSheet sheet2 = EasyExcel.readSheet(2)
                .head(UserModel.class) // 指定sheet0的数据模板
                .registerReadListener(new AnalysisEventListener<UserModel>() {
                    // 每解析一行数据,该方法会被调用一次
                    @Override
                    public void invoke(UserModel demoData, AnalysisContext analysisContext) {
                        System.out.println("解析数据2为:" + demoData.toString());
                    }

                    // 全部解析完成被调用
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        System.out.println("解析2完成...");
                        // 可以将解析的数据保存到数据库
                    }
                }).build();


        excelReader.read(sheet2);

        // 需要自己关闭流操作,在读取文件时会创建临时文件,如果不关闭,会损耗磁盘,严重的磁盘爆掉
        excelReader.finish();
    }

    @Test
    public void testFillExcel() {
        // 根据哪个模板进行填充
        String template = "E:\\home\\easyexcel\\template.xlsx";
        // 填充完成之后的excel
        String fillname = "E:\\home\\easyexcel\\fill.xlsx";
        // 构建数据
        FillData fillData = FillData.builder()
                .name("小米")
                .number(888.888)
                .build();
        // 填充excel 单组数据填充
        EasyExcel.write(fillname).withTemplate(template).sheet(0).doFill(fillData);
    }


    /**
     * 有运算记得添加{.name} {.number}
     */
    @Test
    public void testFillExcel2() {
        // 根据哪个模板进行填充
        String template = "E:\\home\\easyexcel\\template2.xlsx";
        // 填充完成之后的excel
        String fillname = "E:\\home\\easyexcel\\fill2.xlsx";
        // 填充excel 多组数据重复填充
        EasyExcel.write(fillname)
                .withTemplate(template)
                .sheet(0)
                .doFill(getFillData());
    }
    // 构建数据
    private List<FillData> getFillData() {
        List<FillData> fillDataList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            // 构建数据
            FillData fillData = FillData.builder()
                    .name("小米" + i)
                    .number(i * 1000 + 88.88)
                    .build();
            fillDataList.add(fillData);
        }
        return fillDataList;
    }

    /***
     * 多组填充与单组填充
     */
    @Test
    public void testFillExcel3() {
        // 根据哪个模板进行填充
        String template = "E:\\home\\easyexcel\\template3.xlsx";
        // 填充完成之后的excel
        String fillname = "E:\\home\\easyexcel\\fill3.xlsx";

        //多组填充excel

        // 创建填充配置 换行填充
        FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
        // 创建写对象
        ExcelWriter excelWriter =
                EasyExcel.write(fillname).withTemplate(template).build();
        // 创建Sheet对象
        WriteSheet sheet = EasyExcel.writerSheet(0).build();
        // 多组填充excel
        excelWriter.fill(getFillData(), fillConfig, sheet);




        // 单组填充
        HashMap<String, Object> unitData = new HashMap<>();
        unitData.put("nickname", "张三");
        unitData.put("salary", 8088.66);
        excelWriter.fill(unitData, sheet);
        // 关闭流
        excelWriter.finish();
    }


    /***
     * 水平填充
     */
    @Test
    public void testFillExcel4() {
        // 根据哪个模板进行填充
        String template = "E:\\home\\easyexcel\\template4.xlsx";
        // 填充完成之后的excel
        String fillname = "E:\\home\\easyexcel\\fill4.xlsx";
        // 创建填充配置 水平填充
        FillConfig fillConfig = FillConfig.builder()
                .direction(WriteDirectionEnum.HORIZONTAL).build();
        // 创建写对象
        ExcelWriter excelWriter = EasyExcel.write(fillname,
                FillData.class).withTemplate(template).build();
        // 创建Sheet对象
        WriteSheet sheet = EasyExcel.writerSheet(0).build();
        // 多组填充excel
        excelWriter.fill(getFillData(), fillConfig, sheet);
        // 关闭流
        excelWriter.finish();
    }


    /***
     * 复杂模板填充
     */
    @Test
    public void testFillExcel5() {
        // 根据哪个模板进行填充
        String template = "E:\\home\\easyexcel\\template5.xlsx";
        // 填充完成之后的excel
        String fillname = "E:\\home\\easyexcel\\fill5.xlsx";
        // 创建填充配置
        FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
        // 创建写对象
        ExcelWriter excelWriter = EasyExcel.write(fillname)
                .withTemplate(template).build();
        // 创建Sheet对象
        WriteSheet sheet = EasyExcel.writerSheet(0).build();
        /***准备数据 start*****/
        HashMap<String, Object> dateMap = new HashMap<>();
        dateMap.put("date", "2022-10-03");
        HashMap<String, Object> memberMap = new HashMap<>();
        memberMap.put("increaseCount", 500);
        memberMap.put("totalCount", 999);
        HashMap<String, Object> curMonthMemberMap = new HashMap<>();
        curMonthMemberMap.put("increaseCountWeek", 100);
        curMonthMemberMap.put("increaseCountMonth", 200);
        List<MemberVip> memberVips = getMemberVips();
        /***准备数据 end*****/
        excelWriter.fill(dateMap, sheet);
        excelWriter.fill(memberMap, sheet);
        excelWriter.fill(curMonthMemberMap, sheet);
        // 多组填充excel
        excelWriter.fill(memberVips, fillConfig, sheet);
        // 关闭流
        excelWriter.finish();
    }

    private static List<MemberVip> getMemberVips() {
        List<MemberVip> memberVips =new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // 构建数据
            MemberVip memberVip = MemberVip.builder()
                    .id(i+1)
                    .name("会员" + i)
                    .gender("女")
                    .birthday(new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                    .build();
            memberVips.add(memberVip);
        }
        return memberVips;
    }


}
