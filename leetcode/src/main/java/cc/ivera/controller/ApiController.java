package cc.ivera.controller;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.enums.WriteDirectionEnum;
import cn.idev.excel.util.DateUtils;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.write.metadata.fill.FillConfig;
import cn.idev.excel.write.metadata.fill.FillWrapper;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cc.ivera.exception.BusinessException;
import cc.ivera.model.pojo.MobileUser;
import cc.ivera.model.pojo.easyexcel.TestExcel;
import cc.ivera.model.vo.*;
import cc.ivera.model.xml.*;
import cc.ivera.service.ColorService;
import cc.ivera.service.IUserService;
import cc.ivera.service.OcrService;
import cc.ivera.service.TestService;
import cc.ivera.util.DateUtil;
import cc.ivera.util.EmailUtil;
import cc.ivera.util.HideUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;


@Slf4j
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ApiController {

    private final OcrService ocrService;

    private final EmailUtil emailCommonsUtil;

    @Autowired
    private ColorService colorService;


    @GetMapping("/get")
    public ResponseEntity get(@RequestParam("name") String name, @RequestParam("age") Integer age) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("age", age);
        ResponseEntity.status(200);
        return ResponseEntity.ok(map);
    }


    @PostMapping("/post")
    public ResponseEntity post(@RequestBody Map<String, Object> map) {
        ResponseEntity.status(200);
        return ResponseEntity.ok(map);
    }


    @GetMapping("/goods")
    public AjaxResult goods() {
        List<GoodsVo> returnGoodsList = new ArrayList<>();
        GoodsVo goodsVo1 = new GoodsVo();
        goodsVo1.setGoodsId("1");
        goodsVo1.setGoodsName("apple");
        goodsVo1.setGoodsPrice(new BigDecimal("5.00"));
        goodsVo1.setTags(Arrays.asList("好吃", "实惠"));
        returnGoodsList.add(goodsVo1);

        GoodsVo goodsVo2 = new GoodsVo();
        goodsVo2.setGoodsId("2");
        goodsVo2.setGoodsName("banana");
        goodsVo2.setGoodsPrice(new BigDecimal("3.00"));
        goodsVo2.setTags(Arrays.asList("好吃", "实惠"));
        returnGoodsList.add(goodsVo2);

        return AjaxResult.success(returnGoodsList);
    }


    @GetMapping("/finduser/{name}")
    public AjaxResult finduser(@PathVariable("name") String name) {
        List<UsersVo> returnUsersList = new ArrayList<>();
        Date now = new Date();
        UsersVo userVo1 = new UsersVo("1", name + 1, 18, "始皇帝", now);
        UsersVo userVo2 = new UsersVo("2", name + 2, 35, "丞相", now);
        UsersVo userVo3 = new UsersVo("3", name + 3, 50, "商人", now);
        UsersVo userVo4 = new UsersVo("4", name + 4, 48, "王太后", now);
        returnUsersList.add(userVo1);
        returnUsersList.add(userVo2);
        returnUsersList.add(userVo3);
        returnUsersList.add(userVo4);
        return AjaxResult.success(returnUsersList);
    }

    @GetMapping("/cart")
    public AjaxResult cart() {
        List<EsGoodsVo> list = new ArrayList<>();
        EsGoodsVo vo1 = new EsGoodsVo();
        vo1.setId("1");
        vo1.setGoodsImg("https://res.vmallres.com/pimages/uomcdn/CN/pms/202408/displayProduct/10086351592439/1920_1920_a_mobile93AA533EFA1CA108FC4ECD3480CBBF0D.jpg");
        vo1.setGoodsName("HUAWEI手机");
        vo1.setGoodsPrice(new BigDecimal("5288"));
        vo1.setGoodsCount(1);
        vo1.setGoodsState(true);
        list.add(vo1);
        return AjaxResult.success(list);
    }


    @GetMapping("/users")
    public AjaxResult users() {
        Date now = new Date();
        List<UsersVo> returnUsersList = new ArrayList<>();
        UsersVo userVo1 = new UsersVo("1", "嬴政", 18, "始皇帝", now);
        UsersVo userVo2 = new UsersVo("2", "李斯", 35, "丞相", now);
        UsersVo userVo3 = new UsersVo("3", "吕不韦", 50, "商人", now);
        UsersVo userVo4 = new UsersVo("4", "赵姬", 48, "王太后", now);
        returnUsersList.add(userVo1);
        returnUsersList.add(userVo2);
        returnUsersList.add(userVo3);
        returnUsersList.add(userVo4);
        return AjaxResult.success(returnUsersList);
    }


    @GetMapping("/users/{id}")
    public AjaxResult users(@PathVariable("id") String id) {
        Date now = new Date();
        UsersVo userVo1 = new UsersVo("1", "嬴政", 18, "始皇帝", now);
        UsersVo userVo2 = new UsersVo("2", "李斯", 35, "丞相", now);
        UsersVo userVo3 = new UsersVo("3", "吕不韦", 50, "商人", now);
        UsersVo userVo4 = new UsersVo("4", "赵姬", 48, "王太后", now);
        UsersVo ret = new UsersVo();
        if (StringUtils.isNotEmpty(id)) {
            switch (id) {
                case "1":
                    ret = userVo1;
                    break;
                case "2":
                    ret = userVo2;
                    break;
                case "3":
                    ret = userVo3;
                    break;
                case "4":
                    ret = userVo4;
                    break;
                default:
                    ret = userVo1;
                    break;
            }
            return AjaxResult.success(ret);
        } else {
            return AjaxResult.error("用户id不能为空");
        }
    }


    @PostMapping("/users")
    public AjaxResult addUsers(@RequestBody Map<String, Object> map) {
        System.out.println(map);
        return AjaxResult.success();
    }

    @DeleteMapping("/users/{id}")
    public AjaxResult deldUsers(@PathVariable("id") String id) {
        System.out.println(id);
        return AjaxResult.success();
    }


    @GetMapping("/color")
    public ResponseEntity getRandomColors() {
        Map map = new HashMap(16);
        map.put("code", "200");
        map.put("data", colorService.getRandomColors(10));
        return ResponseEntity.ok(map);
    }

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String recognizeImage(@RequestParam("file") MultipartFile file) throws TesseractException, IOException {
        // 调用OcrService中的方法进行文字识别
        return ocrService.recognizeText(file);
    }

    //基于hutool实现数据的脱敏处理
    @GetMapping("/getUser")
    public AjaxResult getUser() {
        MobileUser user = new MobileUser();
        user.setCardId("1234567890123456");
        user.setPhone("13142325003");
        user.setName("Arhi");
        user.setInfo("这是机密文件，该打码打码");
        Object mask = HideUtil.mask(user);
        System.out.println(mask);
        return AjaxResult.success(user);
    }


    @SneakyThrows
    @GetMapping("/excel")
    public void excel(HttpServletResponse response) {
        int size = 2;
        List<TestExcel> testExcels = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TestExcel testExcel = new TestExcel();
            testExcel.setContractNo("HT07040" + (i + 1));
            testExcel.setAddress("青岛" + i + "号基地");
            testExcel.setDateTime("2023-07-05");
            testExcels.add(testExcel);
            List<TestExcel.Item> itemList = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                TestExcel.Item item = new TestExcel.Item();
                item.setName("商品" + (j + 1));
                item.setPrice(new BigDecimal("188").multiply(new BigDecimal(j + 1)));
                itemList.add(item);
            }
            testExcel.setItemList(itemList);
        }
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("测试", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        ClassPathResource classPathResource = new ClassPathResource("easyexcel" + File.separator + "model" + File.separator + "test7.xlsx");
        InputStream stream = classPathResource.getStream();
        // 把excel流给这个对象，后续可以操作
        XSSFWorkbook workbook = new XSSFWorkbook(stream);
        // 设置模板的第一个sheet的名称，名称我们使用合同号
        workbook.setSheetName(0, testExcels.get(0).getContractNo());
        for (int i = 1; i < size; i++) {
            // 剩余的全部复制模板sheet0即可
            workbook.cloneSheet(0, testExcels.get(i).getContractNo());
        }
        // 把workbook写到流里
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        byte[] bytes = baos.toByteArray();
        stream = new ByteArrayInputStream(bytes);
        ExcelWriter excelWriter = FastExcel.write(response.getOutputStream()).withTemplate(stream).build();

        for (TestExcel testExcel : testExcels) {
            WriteSheet writeSheet = FastExcel.writerSheet(testExcel.getContractNo()).build();
            // list不是最后一行，下面还有数据需要填充 就必须设置 forceNewRow=true 但是这个就会把所有数据放到内存 会很耗内存
            FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).direction(WriteDirectionEnum.VERTICAL).build();
            excelWriter.fill(testExcel, writeSheet);
            excelWriter.fill(new FillWrapper("item", testExcel.getItemList()), fillConfig, writeSheet);
        }
        excelWriter.finish();
        baos.close();
        stream.close();
    }


    /**
     * 类似模板操作
     *
     * @param response
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "/pdf", method = RequestMethod.POST)
    public String exportPdf(HttpServletResponse response) throws UnsupportedEncodingException {
        // 1.指定解析器
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        ClassPathResource classPathResource = new ClassPathResource("itext-pdf/test1.pdf");
        String path = classPathResource.getFile().getAbsolutePath();

        String fileName = URLEncoder.encode("测试", "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/pdf");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".pdf");
        OutputStream os = null;
        PdfStamper ps = null;
        PdfReader reader = null;
        try {
            os = response.getOutputStream();
            // 2 读入pdf表单
            reader = new PdfReader(path);
            // 3 根据表单生成一个新的pdf
            ps = new PdfStamper(reader, os);
            // 4 获取pdf表单
            AcroFields form = ps.getAcroFields();
            // 5给表单添加中文字体 这里采用系统字体。不设置的话，中文可能无法显示
            BaseFont bf = BaseFont.createFont("C:/WINDOWS/Fonts/SIMSUN.TTC,1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            form.addSubstitutionFont(bf);
            // 6查询数据================================================
            Map<String, String> data = new HashMap<String, String>();
            data.put("time", DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            data.put("number", "湘A.09771TV");
            data.put("driver", "张三");
            data.put("type", "小车");
            data.put("address", "湖南长沙");
            data.put("payment", "1200.00");
            data.put("managercomment", "同意");
            data.put("leadercomment", "同意");
            data.put("remark", "车胎坏了");
            // 7遍历data 给pdf表单表格赋值
            for (String key : data.keySet()) {
                form.setField(key, data.get(key).toString());
            }
            ps.setFormFlattening(true);
            log.info("*******************PDF导出成功***********************");
        } catch (Exception e) {
            log.error("*******************PDF导出失败***********************");
            e.printStackTrace();
        } finally {
            try {
                ps.close();
                reader.close();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    //https://juejin.cn/post/7126467199660720158 iextpdf
    @Autowired
    private IUserService userService;

    /**
     * pdf命令操作
     *
     * @param response
     */
    @PostMapping("/pdf/v1")
    public void download(HttpServletResponse response) {
        try {
            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-disposition", "attachment;filename=user_pdf_" + System.currentTimeMillis() + ".pdf");
            OutputStream os = response.getOutputStream();
            userService.generateItextPdfDocument(os);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @GetMapping("/sendEmail")
    public AjaxResult sendCommonEmail() {
        String subject = "Hello world!";
        String html = "<html>\n" + "<body>\n" + "<h1>Hello World!</h1>\n" + "</body>\n" + "</html>";
        String[] toMail = new String[]{"2607439502@qq.com"};
        //String[] ccMail = new String[]{"wuhanxue5@163.com"};
        File file = new File("D:\\develop\\code\\cs-java\\leetcode\\output5.docx");
        try {
            emailCommonsUtil.sendEmail(subject, html, true, toMail, null, null, new File[]{file});
        } catch (EmailException | UnsupportedEncodingException e) {
            log.error("邮件发送失败", e);
            throw new BusinessException(ErrorResult.error());
        }
        return AjaxResult.success();
    }


    /**
     * 返回xml格式数据
     *
     * @param name
     * @param uid
     * @return
     */
    @RequestMapping(value = "/xml/v1", produces = MediaType.APPLICATION_XML_VALUE)
    public SystemDept systemDept(@RequestParam(required = false) String name, @RequestParam(required = false) Integer uid) {
        SystemDept systemDept = new SystemDept();
        systemDept.setDeptId("1000");
        systemDept.setDeptName("CPII平台基础架构部");
        systemDept.setLevel(1);
        return systemDept;
    }

    @RequestMapping(value = "/xml/v2", produces = MediaType.APPLICATION_XML_VALUE)
    public Hobbies hobbies(@RequestParam(required = false) String name) {
        Hobbies hobbies = new Hobbies();
        hobbies.setUserId("1001");
        List<Hobby> hobbyList = new ArrayList<>();
        Hobby hobby1 = new Hobby();
        hobby1.setHobbyId("1");
        hobby1.setName("读书");
        hobby1.setDescription("无");
        Hobby hobby2 = new Hobby();
        hobby2.setHobbyId("2");
        hobby2.setName("看电影");
        hobby2.setDescription("无");
        hobbyList.add(hobby1);
        hobbyList.add(hobby2);
        hobbies.setHobbyList(hobbyList);
        return hobbies;
    }


    @RequestMapping(value = "/xml/v3", produces = MediaType.APPLICATION_XML_VALUE)
    public SystemUser jackSonDemoSubs(@RequestParam(required = false) String name) {
        SystemUser systemUser = new SystemUser();
        systemUser.setName("Arhi");
        systemUser.setDob(DateUtil.parseDate("1997-06-06"));
        CurrentRole role1 = new CurrentRole();
        role1.setRoleId("role1");
        role1.setRoleName("CPII平台基础架构部开发人员");
        systemUser.setEmail("knife4j@gmail.com");
        systemUser.setUid("178790997787818271");
        systemUser.setPhoneNum("13112341234");
        List<Hobby> hobbyList = new ArrayList<>();
        Hobby hobby1 = new Hobby();
        hobby1.setHobbyId("1");
        hobby1.setName("读书");
        hobby1.setDescription("无");
        Hobby hobby2 = new Hobby();
        hobby2.setHobbyId("2");
        hobby2.setName("看电影");
        hobby2.setDescription("无");
        hobbyList.add(hobby1);
        hobbyList.add(hobby2);
        systemUser.setHobbies(hobbyList);
        List<CurrentRole> roleList = new ArrayList<>();
        CurrentRole role2 = new CurrentRole();
        role2.setRoleId("role2");
        role2.setRoleName("物业产品部后台开发人员");
        roleList.add(role1);
        roleList.add(role2);
        systemUser.setRoles(roleList);
        systemUser.setRole(role1);
        return systemUser;
    }

    @Autowired
    private TestService testService;

    @PostMapping(value = "/xml/v4", produces = MediaType.APPLICATION_XML_VALUE)
    public String test(@RequestBody TestRequest request) {
        return testService.service(request);
    }

}