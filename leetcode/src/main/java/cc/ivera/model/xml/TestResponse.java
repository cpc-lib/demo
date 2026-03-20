package cc.ivera.model.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "resp")
public class TestResponse {
    //返回码
    @JacksonXmlProperty(localName = "code")
    private String code;
 
    //返回码信息描述
    @JacksonXmlProperty(localName = "msg")
    private String msg;
 
    //可参与活动标志 ：1-可参与 2-不可参与
    @JacksonXmlProperty(localName = "activeflag")
    private String activeFlag;
}