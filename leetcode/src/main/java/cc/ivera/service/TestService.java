package cc.ivera.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cc.ivera.model.xml.TestRequest;
import cc.ivera.model.xml.TestResponse;

import java.util.Objects;

@Slf4j
@Service
public class TestService {

    public String service(TestRequest request) {
        //正常情况
        TestResponse response = new TestResponse();
        response.setCode("200");
        response.setMsg("success");
        response.setActiveFlag("1");
        return javaBeanToXml(response);
    }

    //XML文件头 jdk17 模板字符串
    private static final String XML_HEAD = "<?xml version='1.0' encoding='UTF-8'?>";

    public static String javaBeanToXml(Object obj) {
        String xml = "";
        if (Objects.isNull(obj)) {
            return xml;
        }
        try {
            XmlMapper xmlMapper = new XmlMapper();
            xml = xmlMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("javaBeanToXml error, obj={}, xml={}", obj, xml, e);
            return "";
        }
        // 添加xml文件头
        return XML_HEAD + xml;
    }
}