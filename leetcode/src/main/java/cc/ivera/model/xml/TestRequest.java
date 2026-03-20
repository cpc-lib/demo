package cc.ivera.model.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.Date;

@Data
@JacksonXmlRootElement(localName = "req")
public class TestRequest {

    @JacksonXmlProperty(localName = "tel")
    private String tel;

    @JacksonXmlProperty(localName = "activityId")
    private String productId;
 
    @JacksonXmlProperty(localName = "timestamp")
    private Date timestamp;
}