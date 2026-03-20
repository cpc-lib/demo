package cc.ivera.controller;


import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cc.ivera.annotation.Ip;
import cc.ivera.model.dto.AccountDTO;
import cc.ivera.model.dto.UserDTO;
import cc.ivera.model.pojo.User;
import cc.ivera.model.vo.AjaxResult;
import cc.ivera.service.IpService;
import cc.ivera.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Controller
@RequestMapping("/test")
@CrossOrigin
@Slf4j
public class TestController {

    private String UPLOAD_URL = "";
    @Autowired
    private IpService ipService;

    @GetMapping("/get")
    @ResponseBody
    public ResponseEntity get(HttpServletRequest httpServletRequest) {
        String name = httpServletRequest.getParameter("name");
        String age = httpServletRequest.getParameter("age");
        Map map = new HashMap(16);
        map.put("name", name);
        map.put("age", age);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/post")
    @ResponseBody
    public ResponseEntity post(@RequestBody Map map) {
        System.out.println("----------------------------------------");
        String header = HttpContextUtil.getHttpServletRequest().getHeader("User-Agent");
        //获取ua信息
        UserAgent userAgent = UserAgent.parseUserAgentString(header);
        System.out.println(header);
        System.out.println(userAgent.getBrowser().getName());
        System.out.println(userAgent.getOperatingSystem().getName());
        return ResponseEntity.ok(map);
    }


    @PostMapping("/post/v1")
    @ResponseBody
    public ResponseEntity postV1(@RequestBody Map map) {
        System.out.println("----------------------------------------");
        //String header = HttpContextUtil.getHttpServletRequest().getHeader("User-Agent");
        //获取ua信息
        //UserAgent userAgent = UserAgent.parseUserAgentString(header);
        //System.out.println(header);
        //System.out.println(userAgent.getBrowser().getName());
        //System.out.println(userAgent.getOperatingSystem().getName());
        return ResponseEntity.ok(map);
    }

    @PostMapping("/filewithArgs")
    @ResponseBody
    public ResponseEntity filewithArgs(@RequestParam("file") List<MultipartFile> files, User user) {
        System.out.println(files);
        System.out.println(user);
        Map<String, Object> map = new HashMap<>();
        map.put("code", "200");
        map.put("data", Collections.EMPTY_LIST);
        map.put("msg", "success");
        return ResponseEntity.ok(map);
    }

    @PostMapping("/filewithArgs/v1")
    @ResponseBody
    public ResponseEntity filewithArgsV1(@RequestPart("file") MultipartFile file, User user) {
        System.out.println(file.getOriginalFilename());
        System.out.println(user);
        Map<String, Object> map = new HashMap<>();
        map.put("code", "200");
        map.put("data", Collections.EMPTY_LIST);
        map.put("msg", "success");
        return ResponseEntity.ok(map);
    }

    @PostMapping("/filewithArgs/v11")
    @ResponseBody
    //https://blog.csdn.net/weixin_44745147/article/details/120087484
    public ResponseEntity filewithArgsV11(@RequestParam("file") MultipartFile file, String id) {
        System.out.println(file.getOriginalFilename());
        System.out.println(id);
        Map<String, Object> map = new HashMap<>();
        map.put("code", "200");
        map.put("data", Collections.EMPTY_LIST);
        map.put("msg", "success");
        return ResponseEntity.ok(map);
    }

    @PostMapping("/filewithArgs/v2")
    @ResponseBody
    public ResponseEntity filewithArgsV2(@RequestParam("file") MultipartFile[] files, String userJson) {
        System.out.println(files[0]);
        System.out.println(userJson);
        Map<String, Object> map = new HashMap<>();
        map.put("code", "200");
        map.put("data", Collections.EMPTY_LIST);
        map.put("msg", "success");
        return ResponseEntity.ok(map);
    }

    /**
     * jsonp 处理跨域处理
     *
     * @param callback
     * @param response
     * @throws IOException
     */
    @RequestMapping(value = "/load/data")
    public void loadData2(@RequestParam("callback") String callback, HttpServletResponse response) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("name", "herbert");
        data.put("age", "27");
        // 转json
        Gson gson = new Gson();
        String jsonData = gson.toJson(data);

        //用回调函数名称包裹返回数据
        String result = callback + "(" + jsonData + ")";
        response.getWriter().write(result);
    }

    @RequestMapping("/md5")
    @ResponseBody
    public AjaxResult md5(MultipartFile file) throws IOException {
        if (file == null) {
            return AjaxResult.error("文件为空");
        } else {
            InputStream inputStream = file.getInputStream();
            String md5 = MD5Utils.getStreamMD5String(inputStream);
            AjaxResult result = new AjaxResult();
            Map map = new HashMap();
            map.put("file_name", file.getOriginalFilename());
            map.put("md5", md5);
            return result.success(map);
        }
    }

    @Ip
    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        return "hello";
    }

    @GetMapping("/ip2Region")
    @ResponseBody
    public String ip2Region(HttpServletRequest request) {
        String region = "UNKNOWN";
        try {
            region = ipService.getCityInfo(WebUtil.getIpAddress(request));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        Map<String, Object> map = new HashMap();
        map.put("code", "200");
        map.put("message", "ok");
        map.put("region", region);
        return new Gson().toJson(map);
    }


    @GetMapping("exportExcel")
    public void exportExcel(HttpServletResponse response) {
        List<UserDTO> userListOne = new ArrayList<>();
        UserDTO user1 = new UserDTO();
        user1.setId(1001);
        user1.setUserName("JCccc");
        user1.setUserAge("18");
        userListOne.add(user1);
        List<UserDTO> userListTwo = new ArrayList<>();
        UserDTO user2 = new UserDTO();
        user2.setId(2001);
        user2.setUserName("Mike");
        user2.setUserAge("18");
        userListTwo.add(user2);
        // 多个sheet配置参数
        final List<Map<String, Object>> sheetsList = Lists.newArrayList();
        final String sheetNameOne = "sheet1-1班";
        Map<String, Object> exportMapOne = Maps.newHashMap();
        final ExportParams exportParamsOne = new ExportParams(null, sheetNameOne, ExcelType.HSSF);
        // 以下3个参数为API中写死的参数名 分别是sheet配置/导出类（注解定义）/数据集
        exportMapOne.put("title", exportParamsOne);
        exportMapOne.put("entity", User.class);
        exportMapOne.put("data", userListOne);
        final String sheetNameTwo = "sheet2-2班";
        Map<String, Object> exportMapTwo = Maps.newHashMap();
        final ExportParams exportParamsTwo = new ExportParams(null, sheetNameTwo, ExcelType.HSSF);
        // 以下3个参数为API中写死的参数名 分别是sheet配置/导出类（注解定义）/数据集
        exportMapTwo.put("title", exportParamsTwo);
        exportMapTwo.put("entity", User.class);
        exportMapTwo.put("data", userListTwo);
        // 加入多sheet配置列表
        sheetsList.add(exportMapOne);
        sheetsList.add(exportMapTwo);
        //导出操作
        ExcelUtil.exportExcel(sheetsList, "userList.xls", response);
    }

    @PostMapping("importExcel")
    public void importExcel(@RequestParam("file") MultipartFile multipartFile) {
        try {
            //标题占几行
            Integer titleRows = 0;
            //表头占几行
            Integer headerRows = 1;
            Workbook workBook = ExcelUtil.getWorkbook(multipartFile);
            //获取sheet数量
            int sheetNum = workBook.getNumberOfSheets();
            ImportParams params = new ImportParams();
            //表头在第几行
            params.setTitleRows(titleRows);
            params.setHeadRows(headerRows);
            for (int numSheet = 0; numSheet < sheetNum; numSheet++) {
                String sheetName = workBook.getSheetAt(numSheet).getSheetName();
                //第几个sheet页
                params.setStartSheetIndex(numSheet);
                List<User> result = ExcelUtil.importExcelMore(multipartFile, User.class, params);
                System.out.println("sheetNum=" + numSheet + "   sheetName=" + sheetName);
                System.out.println("导入的数据=" + result.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @RequestMapping("exportUserExcel")
    public void exportUserExcel(HttpServletResponse response) {
        // List<User> userList = userService.queryUserInfo();
        List<UserDTO> userList = new ArrayList<>();
        UserDTO user1 = new UserDTO(1, "a", "12");
        UserDTO user2 = new UserDTO(1, "b", "12");
        UserDTO user3 = new UserDTO(1, "c", "12");
        userList.add(user1);
        userList.add(user2);
        userList.add(user3);
        //导出操作
        ExcelUtil.exportExcel(userList, "用户信息", "sheet1", User.class, "users.xls", response);
    }


    /**
     * 将指定文件打包成zip并下载
     */
    @RequestMapping("exportExcelZipWithFile")
    public void exportExcelZipWithFile(HttpServletResponse response) throws IOException {
        // 这里还是和上面一样
        String[] filePath = new String[]{"D:\\ziptest\\11.xls", "D:\\ziptest\\22.xls"};
        List<File> fileList = new ArrayList<>();
        for (String s : filePath) {
            File file = new File(s);
            fileList.add(file);
        }
        response.setHeader("content-type", "application/octet-stream");
        response.setContentType("application/octet-stream");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=configDetail.zip");
        ZipUtil.downloadZipForFiles(response.getOutputStream(), fileList);
    }

    /**
     * 将excel文件的Byte[]打包成zip并下载
     */
    @RequestMapping("exportExcelZipWithByte")
    public void exportExcelZipWithByte(HttpServletResponse response) throws IOException {
        Map<String, byte[]> fileBufMap = new HashMap<>();
        List<AccountDTO> accountDTOList = new ArrayList<>();
        AccountDTO accountDTO1 = new AccountDTO(1, "1234");
        AccountDTO accountDTO2 = new AccountDTO(2, "12222");
        AccountDTO accountDTO3 = new AccountDTO(3, "1431546");
        accountDTOList.add(accountDTO1);
        accountDTOList.add(accountDTO2);
        accountDTOList.add(accountDTO3);
        //导出操作 1
        byte[] exportAccountDTOExcelBytes = ExcelUtil.getExportExcelBytes(accountDTOList, "账号信息", "sheet1", AccountDTO.class, "accountDTOs.xls", response);
        List<UserDTO> userList = new ArrayList<>();
        UserDTO user1 = new UserDTO(1, "a", "12");
        UserDTO user2 = new UserDTO(1, "b", "12");
        UserDTO user3 = new UserDTO(1, "c", "12");
        userList.add(user1);
        userList.add(user2);
        userList.add(user3);
        //导出操作
        byte[] exportUserExcelBytes = ExcelUtil.getExportExcelBytes(userList, "用户信息", "sheet1", User.class, "users.xls", response);
        fileBufMap.put("accountDTOs.xls", exportAccountDTOExcelBytes);
        fileBufMap.put("users.xls", exportUserExcelBytes);
        response.setHeader("content-type", "application/octet-stream");
        response.setContentType("application/octet-stream");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=configDetail.zip");
        ZipUtil.downloadZipForByteMore(response.getOutputStream(), fileBufMap);
    }


    /***************************接收数组参数*******************************/

    /**
     * post请求获取数组参数 参数之间采用英文,间隔 需添加@RequestParam
     *
     * @param ids
     * @return
     */
    @PostMapping("/dataList")
    @ResponseBody
    public AjaxResult dataListV1(@RequestParam("ids") List<String> ids) {
        return AjaxResult.success(ids);
    }


    /**
     * post请求获取数组参数 参数之间采用英文,间隔 必须添加@RequestParam
     *
     * @param ids
     * @return
     */
    @GetMapping("/dataList")
    @ResponseBody
    public AjaxResult dataListV2(@RequestParam("ids") List<String> ids) {
        return AjaxResult.success(ids);
    }


    /**
     * [1,2,3,4,5,6]
     *
     * @param ids
     * @return
     */
    @PostMapping("/dataList/json")
    @ResponseBody
    public AjaxResult dataListV3(@RequestBody List<String> ids) {
        return AjaxResult.success(ids);
    }

}
