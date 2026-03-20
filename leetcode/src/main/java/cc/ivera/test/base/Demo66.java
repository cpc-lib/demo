package cc.ivera.test.base;

import cn.hutool.core.io.resource.ClassPathResource;
import org.apache.commons.mail.EmailException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import cc.ivera.util.ConvertUtil;
import cc.ivera.util.EmailUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.StringJoiner;

@SpringBootTest(classes = cc.ivera.Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class Demo66 {


    @Autowired
    private EmailUtil emailUtil;

    /**
     * pdf转图片测试
     */
    @Test
    public void test1() {
        ClassPathResource classPathResource = new ClassPathResource("itext-pdf/1602.pdf");
        String absolutePath = classPathResource.getAbsolutePath();
        File file = new File(absolutePath);
        String targetAddress = file.getParentFile().getAbsolutePath();
        ConvertUtil.pdf2png(targetAddress, absolutePath, "png", 255);
    }


    /**
     * 将test1生成的图片合成新的pdf
     */
    @Test
    public void test2() {

        ClassPathResource classPathResource1 = new ClassPathResource("itext-pdf/1602_1.png");
        String absolutePath1 = classPathResource1.getAbsolutePath();
        ClassPathResource classPathResource2 = new ClassPathResource("itext-pdf/1602_2.png");
        String absolutePath2 = classPathResource2.getAbsolutePath();

        //文件需要存在
        ClassPathResource classPathResource = new ClassPathResource("itext-pdf/target.pdf");
        String target = classPathResource.getAbsolutePath();

        StringJoiner stringJoiner = new StringJoiner(",");
        stringJoiner.add(absolutePath1);
        stringJoiner.add(absolutePath2);
        String filePaths = stringJoiner.toString();
        ConvertUtil.imgOfPdf(target, filePaths);
    }

    /**
     * word转pdf文件
     *
     * @throws Exception
     */
    @Test
    public void test3() throws Exception {
        String wordPath = "D:\\home\\test\\基于微控制器的信号发生器设计.docx";
        String pdfPath = "D:\\home\\test\\基于微控制器的信号发生器设计.pdf";
        ConvertUtil.word2Pdf(wordPath, pdfPath);
    }

    /**
     * 发送带附件的邮件
     */
    @Test
    public void test4() {
        String subject = "这是一个测试标题";
        String html = "<h1>统计数据如下所示：</h1>" +
                "<table border=\"1\">\n" +
                "  <tr>\n" +
                "    <th>月度销售额</th>\n" +
                "    <th>年度销售额</th>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>10000</td>\n" +
                "    <td>2000000</td>\n" +
                "  </tr>\n" +
                "</table>";
        String[] toMail = new String[]{"e2607439502@163.com"};
        File file = new File("D:\\develop\\code\\cs-java\\leetcode\\output5.docx");
        try {
            emailUtil.sendEmail(subject, html, true, toMail, null, null, new File[]{file});
        } catch (EmailException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
