package cc.ivera.test.base;

import cn.hutool.core.bean.BeanUtil;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.config.ConfigureBuilder;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.deepoove.poi.util.PoitlIOUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import cc.ivera.model.pojo.DataForm;
import cc.ivera.model.pojo.VoteInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class Demo64 {

    /**
     * 测试导出投票表 难点复选勾选
     */
    @Test
    public void test1() {


        /**
         *  1. 获取所有投票人的列表数据
         *    此处使用测试数据，实际项目中需要自己从数据库查询
         * */
        List<VoteInfo> infoList = new ArrayList<VoteInfo>() {{

            add(new VoteInfo("张三", "1", "没有意见"));
            add(new VoteInfo("李四", "1", "同意"));
            add(new VoteInfo("王五", "3", "拒绝，不同意"));
            add(new VoteInfo("宋小宝", "4", "我回避！不做任何评价"));
            add(new VoteInfo("海燕", "2", "我认为需要续议，日后再重新讨论"));
        }};

        StringBuffer allOpinion = new StringBuffer();
        for (VoteInfo info : infoList) {
            //拼接所有投票人的意见  并换行
            allOpinion.append(info.getVoterName() + "：" + info.getVoteOpinion() + "\n");
        }

        /**
         *  2. 获取并设置表单数据  此处为测试数据  meetingType和passFlag默认设置为1
         *  复选框 meetingType  会议类型 1、股东会 2、董事会  3、合伙人会 4、其他
         *  复选框 passFlag  是否通过 1、同意  2、续议  3、拒绝
         * */

        DataForm dataForm = new DataForm("A公司", "2024", "5", "20", "如何走向人生巅峰迎娶白富美",
                "1", "1", allOpinion.toString(), "A", "2024-05-20");


        /**
         * 3. 数据map  用于渲染word表单的数据 将表单数据对象存入map中
         */
        Map<String, Object> dataMap = BeanUtil.beanToMap(dataForm);
        //需要循环的表单数据
        dataMap.put("dataTable", infoList);

        /**
         * 4. Configure类是该库中的一个配置类，其作用是提供了一些全局的配置选项
         * (1) useSpringEL() 开启El表达式{{ }}  word模板中的数据就以这个表达式传递数据 例如：{{companyName}};也可以调用buildGramer("${", "}") 可以修改模板为${}
         * (2) bind()  绑定标记需要循环的数据
         * (3) 实现表格行循环的策略 HackLoopTableRenderPolicy 不同poi版本实现行循环的策略不一样；当前案例是poi-tl 1.9.1版本
         *      1.9.x版本：HackLoopTableRenderPolicy  1.10.x以后的版本：LoopRowTableRenderPolicy
         *
         */
        ConfigureBuilder configureBuilder = Configure.builder().useSpringEL().bind("dataTable", new LoopRowTableRenderPolicy());
        Configure config = configureBuilder.build();
        InputStream is = null;
        try {
            /**
             *  5. word模板渲染数据  wordForm.docx为模板，本案例放在了项目根目录的wordTemplates下
             */
            is = new ClassPathResource("poi-tl/test5.docx").getInputStream();
            XWPFTemplate template = XWPFTemplate.compile(is, config).render(dataMap);

            /** 6.  根据模板生成word文件的指定路径 */
            //生成 意见反馈表word 到指定路径
            OutputStream out = new FileOutputStream("output5.docx");
            /**  若是直接访问下载打开 可以通过如下方式实现 不需要设置文件路径
             *  String fileName = URLEncodeUtil.encode("程序猿会议意见反馈表");
             *  response.setContentType("application/octet-stream;charset=utf-8");
             *  response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".docx");
             *  OutputStream out = response.getOutputStream();
             */

            /**
             *  7. 文件输出
             */
            template.writeAndClose(out);
            out.flush();
            PoitlIOUtils.closeQuietlyMulti(template, out);
        } catch (IOException e) {
            log.error("生成意见反馈表失败！", e);
        } finally {
            if (null != is) {
                try {
                    //最后别忘记了关闭流
                    is.close();
                } catch (IOException e) {
                    log.error("关闭流失败！", e);
                }
            }
        }

    }
}

