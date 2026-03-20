package cc.ivera.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.sun.deploy.net.URLEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import cc.ivera.domain.UserExcel;
import cc.ivera.domain.base.AjaxResult;
import cc.ivera.service.UserService;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Resource(name = "userService")
    private UserService userService;

    /*模拟效果，使用apifox进行测试*/
    @PostMapping("/export")
    public void exportExcel(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // 设置响应头
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 设置防止中文名乱码
        String filename = URLEncoder.encode("员工信息", "utf-8");
        // 文件下载方式(附件下载还是在当前浏览器打开)
        response.setHeader("Content-disposition", "attachment;filename=" +
                filename + ".xlsx");
        // 构建写入到excel文件的数据
        List<UserExcel> userExcels = new ArrayList<>();
        UserExcel userExce1 = new UserExcel(1001, "张三", "男", 1333.33,
                new Date());
        UserExcel userExce2 = new UserExcel(1002, "李四", "男", 1356.83,
                new Date());
        UserExcel userExce3 = new UserExcel(1003, "王五", "男", 1883.66,
                new Date());
        UserExcel userExce4 = new UserExcel(1004, "赵六", "男", 1393.39,
                new Date());
        userExcels.add(userExce1);
        userExcels.add(userExce2);
        userExcels.add(userExce3);
        userExcels.add(userExce4);

        // 写入数据到excel(写入数据到单个sheet)
        EasyExcel.write(response.getOutputStream(), UserExcel.class)
                .sheet("用户信息")
                .doWrite(userExcels);


    }


    @PostMapping("/import")
    @ResponseBody
    public AjaxResult importExel(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return null;
    }


    /*模拟效果，使用apifox进行测试 文件员工信息表*/
    @PostMapping("/uploadExcel")
    @ResponseBody
    public AjaxResult uploadExcel(@RequestParam("file") MultipartFile file) throws Exception {
        // 获取上传的文件流
        InputStream inputStream = file.getInputStream();
        // 读取Excel
        EasyExcel.read(inputStream, UserExcel.class,
                new AnalysisEventListener<UserExcel>() {
                    // 每解析一行数据,该方法会被调用一次
                    @Override
                    public void invoke(UserExcel data, AnalysisContext analysisContext) {
                        System.out.println("解析数据为:" + data.toString());
                    }

                    // 全部解析完成被调用
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        System.out.println("解析完成...");
                        // 可以将解析的数据保存到数据库
                    }
                }).sheet().doRead();
        return AjaxResult.success("文件上传成功");
    }

}
