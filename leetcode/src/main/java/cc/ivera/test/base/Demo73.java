package cc.ivera.test.base;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import java.util.Iterator;

public class Demo73 {

    @Test
    public void test1() throws DocumentException {
        String xmlString = """
                <?xml version="1.0" encoding="utf-8"?>
                <responsedata>
                  <resultcode>xxx</resultcode>
                  <resultdesc>xxx</resultdesc>
                </responsedata> 
                """;
        org.dom4j.Document document = DocumentHelper.parseText(xmlString);
        //获取根节点,在例子中就是responsedata节点
        Element rootElement = document.getRootElement();
        //获取根节点下的某个元素
        Element resultcode = rootElement.element("resultcode");
        Element resultdesc = rootElement.element("resultdesc");
        //getData返回元素的数据
        String resultcodeData = (String) resultcode.getData();
        String data = (String) resultdesc.getData();
        //遍历所有子节点
        for (Iterator i = rootElement.elementIterator(); i.hasNext(); ) {
            Element next = (Element) i.next();
            System.out.println(next.getName() + "：" + next.getData());
        }
        //遍历某个子节点，如resultcode
        for (Iterator i = rootElement.elementIterator("resultcode"); i.hasNext(); ) {
            Element next = (Element) i.next();
            System.out.println(next.getName() + "：" + next.getData());
        }
    }


    @Test
    public void test2() throws DocumentException {
        String xmlString = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FDLibInfoList version="2.0" xmlns="http://www.hikvision.com/ver20/XMLSchema">
                <FDLibInfo>
                <id>1</id>
                <name>sci3</name>
                <FDID>2</FDID>
                </FDLibInfo>
                </FDLibInfoList>
                """;
        org.dom4j.Document document = DocumentHelper.parseText(xmlString);
        Element rootElement = document.getRootElement();
        int count = 0;
        String text = "";
        for (Iterator i = rootElement.elementIterator("FDLibInfo"); i.hasNext(); ) {
            Element next = (Element) i.next();
            Element fdid = next.element("FDID");
            text = fdid.getText();
            count++;
            break;
        }
        System.out.println(text);
    }

    @Test
    public void test3() throws DocumentException {
        String xmlString = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ResponseStatus version="2.0" xmlns="http://www.hikvision.com/ver20/XMLSchema">
                <requestURL></requestURL>
                <statusCode>1</statusCode>
                <statusString>OK</statusString>
                <subStatusCode>ok</subStatusCode>
                </ResponseStatus>
                """;
        org.dom4j.Document document = DocumentHelper.parseText(xmlString);
        Element rootElement = document.getRootElement();
        Element statusCode = rootElement.element("statusCode");
        String text = statusCode.getText();
        System.out.println(text);


    }
}
