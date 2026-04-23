package cc.ivera.test;

import cc.ivera.domain.pojo.*;
import cc.ivera.service.UserService;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelReader;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.enums.WriteDirectionEnum;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.read.builder.ExcelReaderBuilder;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.write.metadata.fill.FillConfig;
import cn.idev.excel.write.metadata.fill.FillWrapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@SpringBootTest(classes = cc.ivera.Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class Demo {

    @Resource
    private UserService userService;

    //https://blog.csdn.net/u013044713/article/details/120249233

    //使用easyexcel写入数据 直接写入数据
    @Test
    public void test1() throws IOException {
        //1.从resource中读取文件
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test1.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 向Excel中写入数据 也可以通过 head(Class<?>) 指定数据模板
        List<User> userList = userService.getUserList();
        FastExcel.write(absolutePath, User.class).sheet("用户信息").doWrite(userList);
    }

    //使用easyexcel写入数据sheet1
    @Test
    public void test2() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test2.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 创建ExcelWriter对象
        ExcelWriter excelWriter = FastExcel.write(absolutePath, User.class).build();
        // 创建Sheet对象
        WriteSheet writeSheet = FastExcel.writerSheet("用户信息").build();
        // 向Excel中写入数据
        excelWriter.write(userService.getUserList(), writeSheet);
        // 关闭流
        excelWriter.finish();
    }


    /**
     * 字段排除
     */
    @Test
    public void test3() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test3.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        Set<String> excludeFields = new HashSet<>();
        excludeFields.add("email");
        excludeFields.add("idCard");
        FastExcel.write(absolutePath, User.class).excludeColumnFiledNames(excludeFields).sheet("用户信息").doWrite(userService.getUserList());
    }

    /**
     * 仅仅导出需要的字段
     */
    @Test
    public void test4() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test4.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 设置要导出的字段
        Set<String> includeFields = new HashSet<>();
        includeFields.add("rank");
        includeFields.add("createTime");
        includeFields.add("city");
        // 写Excel
        FastExcel.write(absolutePath, User.class).includeColumnFiledNames(includeFields).sheet("用户信息").doWrite(userService.getUserList());
    }

    /**
     * 使用index属性指定列的顺序
     */
    @Test
    public void test5() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test5.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 向Excel中写入数据
        FastExcel.write(absolutePath, User.class).sheet("用户信息").doWrite(userService.getUserList());
    }

    /**
     * easyexcel分组写入
     */
    @Test
    public void test6() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test6.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        List<ComplexHeadUser> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ComplexHeadUser user = ComplexHeadUser.builder().userId(i).userName("大哥" + i).hireDate(new Date()).build();
            users.add(user);
        }
        // 向Excel中写入数据
        FastExcel.write(absolutePath, ComplexHeadUser.class).sheet("用户信息").doWrite(users);
    }


    /**
     * 写入数据到不同的sheet
     */
    @Test
    public void test7() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test7.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 创建ExcelWriter对象
        ExcelWriter excelWriter = FastExcel.write(absolutePath, User.class).build();
        // 向Excel的不同Sheet写入相同数据 可以做数据分页处理比如 一个sheet写入1000条数据
        for (int i = 0; i < 2; i++) {
            // 创建Sheet对象
            WriteSheet writeSheet = FastExcel.writerSheet("用户信息" + i).build();
            excelWriter.write(userService.getUserList(), writeSheet);
        }
        // 关闭流
        excelWriter.finish();
    }


    /**
     * 时间格式化,金额四舍五入,转换器
     */
    @Test
    public void test8() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test8.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 向Excel中写入数据
        FastExcel.write(absolutePath, UserDemo.class).sheet("用户信息").doWrite(getUserData());
    }


    private static List<UserDemo> getUserData() {
        List<UserDemo> temp = new ArrayList<>();
        UserDemo userDemo = new UserDemo();
        userDemo.setUserId(1);
        userDemo.setUserName("Arhi");
        userDemo.setGender("1");
        userDemo.setHireDate(new Date());
        userDemo.setSalary(10000.067);
        temp.add(userDemo);
        return temp;
    }

    /**
     * 写入图片数据到excel中
     */
    @Test
    public void test9() throws IOException {

        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test9.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();

        // 图片位置
        ClassPathResource image = new ClassPathResource("easyexcel/image/test9.png");
        String imagePath = image.getFile().getAbsolutePath();

        // 网络图片
        URL url = new URL("https://cn.bing.com/th?id=OHR.TanzaniaBeeEater_ZH-CN3246625733_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp");

        File file = new File(imagePath);

        InputStream inputStream = new FileInputStream(imagePath);


        // 将图片读取到二进制数据中
        byte[] bytes = new byte[(int) new File(imagePath).length()];

        inputStream.read(bytes, 0, bytes.length);


        List<ImageData> imageDataList = new ArrayList<>();

        // 添加要写入的图片模型
        for (int i = 0; i < 1; i++) {
            // 创建数据模板
            ImageData imageData = ImageData.builder().file(file)
                    //自动关闭
                    .inputStream(inputStream).str(imagePath).byteArr(bytes).url(url).build();
            imageDataList.add(imageData);
        }
        // 写数据
        FastExcel.write(absolutePath, ImageData.class).sheet("图片").doWrite(imageDataList);
    }

    /**
     * 样式测试 使用频率低
     */
    @Test
    public void test10() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test10.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 构建数据
        List<StyleData> dataList = new ArrayList<>();
        StyleData data = StyleData.builder().string("字符串").date(new Date()).doubleData(888.88).build();
        dataList.add(data);
        // 向Excel中写入数据
        FastExcel.write(absolutePath, StyleData.class).sheet("行高和列宽测试").doWrite(dataList);
    }

    /**
     * 使用频率低
     */
    @Test
    public void test11() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test11.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 构建数据
        List<MergeData> dataList = new ArrayList<>();
        MergeData data = MergeData.builder().string("字符串").date(new Date()).doubleData(888.88).build();
        dataList.add(data);
        // 向Excel中写入数据
        FastExcel.write(absolutePath, MergeData.class).sheet("单元格合并测试").doWrite(dataList);
    }

    /**
     * 从excel中读取数据
     */
    @Test
    public void test12() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/input/test1.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 创建ExcelReaderBuilder对象
        ExcelReaderBuilder readerBuilder = EasyExcel.read();
        // 获取文件对象
        readerBuilder.file(absolutePath);
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
                System.out.println("解析数据为:" + demoData.toString());
            }

            // 全部解析完成被调用
            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                System.out.println("解析完成...");
            }
        });
        readerBuilder.doReadAll();
    }


    /**
     * 读取第一页的数据并解析成对象
     */
    @Test
    public void test13() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/input/test1.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 读取excel
        EasyExcel.read(absolutePath, User.class, new AnalysisEventListener<User>() {
            // 每解析一行数据,该方法会被调用一次
            @Override
            public void invoke(User demoData, AnalysisContext analysisContext) {
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


    /**
     * 读取所有页的数据并解析成对象
     */
    @Test
    public void test14() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/input/test1.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 读取excel
        EasyExcel.read(absolutePath, User.class, new AnalysisEventListener<User>() {
            // 每解析一行数据,该方法会被调用一次
            @Override
            public void invoke(User demoData, AnalysisContext analysisContext) {
                System.out.println("解析数据为:" + demoData.toString());
            }

            // 解析完一个sheet后被调用
            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                System.out.println("解析完成...");
            }
        }).doReadAll(); // 读取全部sheet
    }


    /**
     * 读取所有sheet数据并解析成对象
     */
    @Test
    public void test15() throws IOException {
        // 读取的excel文件路径
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/input/test1.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 创建ExcelReader对象
        ExcelReader excelReader = EasyExcel.read(absolutePath, User.class, new AnalysisEventListener<User>() {
            // 每解析一行数据,该方法会被调用一次
            @Override
            public void invoke(User demoData, AnalysisContext analysisContext) {
                System.out.println("解析数据为:" + demoData.toString());
            }

            // 全部解析完成被调用
            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                System.out.println("解析完成...");
                // 可以将解析的数据保存到数据库
            }
        }).build();
        excelReader.readAll(); // 读所有sheet
        excelReader.finish();
    }

    /**
     * 一般使用填充模板数据 {id}
     */
    @Test
    public void test16() throws IOException {
        // 根据哪个模板进行填充
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/model/test1.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 填充完成之后的excel
        ClassPathResource output = new ClassPathResource("easyexcel/template/template1.xlsx");
        String outputPath = output.getFile().getAbsolutePath();
        // 构建数据
        List<User> userList = userService.getUserList();
        User User = userList.get(0);
        // 填充excel 单组数据填充
        FastExcel.write(outputPath).withTemplate(absolutePath).sheet(0).doFill(User);
    }


    /**
     * 参与运算的模板导出 {.name} {.number} 指定sheet 没有分页处理哦
     */
    @Test
    public void test17() throws IOException {
        // 根据哪个模板进行填充
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/model/test2.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 填充完成之后的excel
        ClassPathResource output = new ClassPathResource("easyexcel/template/template2.xlsx");
        String outputPath = output.getFile().getAbsolutePath();
        // 填充excel 多组数据重复填充
        FastExcel.write(outputPath).withTemplate(absolutePath).sheet(0).doFill(getFillData());
    }

    // 构建数据
    private List<FillData> getFillData() {
        List<FillData> fillDataList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            // 构建数据
            FillData fillData = FillData.builder().name("小米" + i).number(i * 1000 + 88.88).build();
            fillDataList.add(fillData);
        }
        return fillDataList;
    }
    


    /***
     * 多组填充与单组填充 没有分页处理
     */
    @Test
    public void test19() throws IOException {
        // 根据哪个模板进行填充
        // 根据哪个模板进行填充
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/model/test3.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 填充完成之后的excel
        ClassPathResource output = new ClassPathResource("easyexcel/template/template3.xlsx");
        String outputPath = output.getFile().getAbsolutePath();

        //多组填充excel
        // 创建填充配置 换行填充 （多组填充开关）
        FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
        // 创建写对象
        ExcelWriter excelWriter = FastExcel.write(outputPath).withTemplate(absolutePath).build();
        // 创建Sheet对象
        WriteSheet sheet = FastExcel.writerSheet(0).build();
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

    /**
     * 水平填充
     */
    @Test
    public void test20() throws IOException {

        // 根据哪个模板进行填充
        // 根据哪个模板进行填充
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/model/test4.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 填充完成之后的excel
        ClassPathResource output = new ClassPathResource("easyexcel/template/template4.xlsx");
        String outputPath = output.getFile().getAbsolutePath();

        // 创建填充配置 水平填充
        FillConfig fillConfig = FillConfig.builder().direction(WriteDirectionEnum.HORIZONTAL).build();
        // 创建写对象
        ExcelWriter excelWriter = FastExcel.write(outputPath, FillData.class).withTemplate(absolutePath).build();
        // 创建Sheet对象
        WriteSheet sheet = FastExcel.writerSheet(0).build();
        // 多组填充excel
        excelWriter.fill(getFillData(), fillConfig, sheet);
        // 关闭流
        excelWriter.finish();
    }

    /**
     * 复杂模板填充
     */
    @Test
    public void test21() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/model/test5.xlsx");
        String template = classPathResource.getFile().getAbsolutePath();
        // 填充完成之后的excel
        ClassPathResource output = new ClassPathResource("easyexcel/template/template5.xlsx");
        String fillname = output.getFile().getAbsolutePath();
        // 创建填充配置
        FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
        // 创建写对象
        ExcelWriter excelWriter = FastExcel.write(fillname).withTemplate(template).build();
        // 创建Sheet对象
        WriteSheet sheet = FastExcel.writerSheet(0).build();
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
        List<MemberVip> memberVips = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // 构建数据
            MemberVip memberVip = MemberVip.builder().id(i + 1).name("会员" + i).gender("女").birthday(new SimpleDateFormat("yyyy-MM-dd").format(new Date())).build();
            memberVips.add(memberVip);
        }
        return memberVips;
    }

    /***
     *  模板导出+分页如何实现呢 获取到存储的所有数据然后进行操作每一个sheet进行处理就好
     *  https://blog.csdn.net/xiyang_1990/article/details/131626343
     *  根据此进行的仿写
     */
    @Test
    public void test22() throws IOException {

        // 根据哪个模板进行填充
        cn.hutool.core.io.resource.ClassPathResource classPathResource = new cn.hutool.core.io.resource.ClassPathResource("easyexcel/model/test6.xlsx");
        InputStream stream = classPathResource.getStream();


        // 填充完成之后的excel
        ClassPathResource output = new ClassPathResource("easyexcel/template/template6.xlsx");
        String outputPath = output.getFile().getAbsolutePath();
        //FileOutputStream fileOutputStream = new FileOutputStream(outputPath);

        // 构建数据
        List<User> totalUsers = userService.getUserList();

        //获取到分页后的excel文档
        List<UserExcel> UserExcels = UserExcel(totalUsers, 100);

        XSSFWorkbook workbook = new XSSFWorkbook(stream);


        workbook.setSheetName(0, UserExcels.get(0).getSheetName());

        Integer totalPages = getPageNum(totalUsers, 100);

        //每个sheet写入数据
        for (int pageNum = 1; pageNum < totalPages; pageNum++) {
            //进行sheet克隆处理
            workbook.cloneSheet(0, UserExcels.get(pageNum).getSheetName());
        }

        //将workbook写入流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        workbook.write(baos);

        byte[] bytes = baos.toByteArray();

        stream = new ByteArrayInputStream(bytes);

        ExcelWriter excelWriter = FastExcel.write(outputPath).withTemplate(stream).build();

        for (UserExcel UserExcel : UserExcels) {
            WriteSheet writeSheet = FastExcel.writerSheet(UserExcel.getSheetName()).build();
            FillConfig fillConfig = FillConfig.builder().forceNewRow(true).direction(WriteDirectionEnum.VERTICAL).build();
            excelWriter.fill(UserExcel, writeSheet);
            excelWriter.fill(new FillWrapper("User", UserExcel.getItemList()), fillConfig, writeSheet);
        }
        //手动关闭
        excelWriter.finish();
        //释放资源
        baos.close();

        stream.close();
    }

    private List<UserExcel> UserExcel(List<User> totalUsers, Integer pageSize) {
        List<UserExcel> UserExcels = new ArrayList<>();
        Integer totalPages = getPageNum(totalUsers, pageSize);
        for (Integer i = 0; i < totalPages; i++) {
            List<User> pageData = getPageData(totalUsers, i, pageSize);
            List<UserExcel.User> itemList = new ArrayList<>();
            for (User pageDatum : pageData) {
                UserExcel.User item = new UserExcel.User();
                BeanUtils.copyProperties(pageDatum, item);
                itemList.add(item);
            }
            UserExcel UserExcel = new UserExcel();
            UserExcel.setSheetName("sheet" + (i + 1));
            UserExcel.setItemList(itemList);
            UserExcels.add(UserExcel);
        }
        return UserExcels;
    }


    private static void pageWrite(ExcelWriter excelWriter, List<User> totalUsers, Integer pageNum, Integer pageSize, FillConfig fillConfig, WriteSheet writeSheet) {
        List<User> users = getPageData(totalUsers, pageNum, pageSize);
        // 多组填充excel
        excelWriter.fill(new FillWrapper(users), fillConfig, writeSheet);
    }

    private static List<User> getPageData(List<User> totalUsers, Integer pageNum, Integer pageSize) {
        List<User> page = totalUsers.stream().skip(pageNum * pageSize).limit(pageSize).collect(Collectors.toList());
        return page;
    }

    private static Integer getPageNum(List<User> totalUsers, Integer pageSize) {
        int totalRecords = totalUsers.size();
        int fullPages = totalUsers.size() / pageSize;
        // 如果有剩余数据（余数不为0），则增加一页
        if (totalRecords % pageSize != 0) {
            fullPages++;
        }
        return fullPages;
    }

}
