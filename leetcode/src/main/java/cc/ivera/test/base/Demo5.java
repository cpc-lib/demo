package cc.ivera.test.base;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Random;

/**
 * @author e2607 生成随机字符串
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class Demo5 {
    @Test
    public void test001() {
        Random random = new Random();
        String[] str = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            strBuilder.append(str[random.nextInt(str.length)]);
        }
        String s = strBuilder.toString();
        System.out.println(s);
    }

    @Test
    public void test002() {
        String roomNumId = "1815288372350709760;";
        String[] split = roomNumId.split(";");
        System.out.println(split[0]);
    }

    @Test
    public void test003() {
        String roomNumId = "1815288372350709760";
        String[] split = roomNumId.split(";");
        System.out.println(split.length);
    }


    @Test
    public void test004() {
        String roomNumId = "";
        String[] split = roomNumId.split(";");
        System.out.println("".equals(split[0]));
    }


    public static void main(String[] args) {
        //test005();
        test006();
    }


    public static void test005() {
        BigDecimal bigDecimal1 = new BigDecimal("0.00");
        BigDecimal bigDecimal2 = new BigDecimal("0.01");
        int i = bigDecimal1.compareTo(bigDecimal2);

        BigDecimal notPaidAmountDecimal = bigDecimal1.subtract(bigDecimal2);
        System.out.println(notPaidAmountDecimal);
        System.out.println(i);
    }

    public static void test006() {
        String json = """
                {
                    "code": "E0000",
                    "describe": "获取成功",
                    "result": [
                        {
                            "additionalElementList": [],
                            "additionalElementName": "",
                            "address": "",
                            "allElectronicInvoiceNumber": "20882408051806541460",
                            "bankAccount": "",
                            "buyerManagerName": "",
                            "checker": "李四1332323232",
                            "clerk": "张晓12345",
                            "clerkId": "",
                            "createTime": 1722852414000,
                            "deptId": "",
                            "digitAccount": "13323333333",
                            "emailNotifyStatus": "4",
                            "extensionNumber": "5556",
                            "imgUrls": "https://inv.jss.com.cn/fp2/LwTegTzvN8KdsIPJIcAeXEzV3SXrl0d6MDV70ImviRBzZnCdM-Z88eMflWMq8he_zk6FYZAgcAQkyO8ZDfxwFQ.jpg",
                            "invoiceDate": 1722823200000,
                            "invoiceType": "1",
                            "listFlag": "0",
                            "listName": "",
                            "managerCardNo": "",
                            "managerCardType": "",
                            "naturalPersonFlag": 0,
                            "notifyEmail": "",
                            "ofdUrl": "https://inv.jss.com.cn/fp2/LwTegTzvN8KdsIPJIcAeXEQrIF_JB6i3iLD5KZzzdN2OF3CLCFQaOWZ8GbWR87Am8TTM7vV1Lp-9d1gGzYKwTg.ofd",
                            "oldInvoiceCode": "",
                            "oldInvoiceNo": "",
                            "orderAmount": "0.01",
                            "payee": "张三",
                            "phone": "",
                            "phoneNotifyStatus": "4",
                            "productOilFlag": 0,
                            "proxyInvoiceFlag": "0",
                            "redReason": "",
                            "remark": "",
                            "requestSrc": "0",
                            "saleName": "航信培训企业199",
                            "salerAccount": "浙江桐庐农村商业银行股份有限公司城南支行201000211280500",
                            "salerAddress": "浙江省杭州市西湖区双龙街199号杭政储出【2013】51号地块商业商务用房5＃楼7层701-708室",
                            "salerTaxNum": "339901999999199",
                            "salerTel": "0571-81395853",
                            "specificFactor": 0,
                            "stateUpdateTime": 1722852415000,
                            "telephone": "",
                            "terminalNumber": "",
                            "updateTime": 1722852414000,
                            "serialNo": "24080518065402849336",
                            "orderNo": "1820393299983601664",
                            "status": "2",
                            "statusMsg": "开票完成（最终状态）",
                            "failCause": "",
                            "pdfUrl": "https://inv.jss.com.cn/fp2/LwTegTzvN8KdsIPJIcAeXHhEEh6jzzzoOzBDhF6uKpauUUDm3pjwF6uZmWiNu4ALsuGOh2SelgqQx9QgXDbcWw.pdf",
                            "pictureUrl": "nnfp.jss.com.cn/955r7q2Jk2-R37K",
                            "invoiceTime": 1722852414000,
                            "invoiceCode": "",
                            "invoiceNo": "20882408051806541460",
                            "exTaxAmount": "0.01",
                            "taxAmount": "0.00",
                            "payerName": "黄",
                            "payerTaxNo": "",
                            "invoiceKind": "电子发票(普通发票)",
                            "checkCode": "",
                            "qrCode": "01,32,,20882408051806541460,0.01,20240805,,649C",
                            "machineCode": "",
                            "cipherText": "",
                            "invoiceItems": [
                                {
                                    "deduction": "0.00",
                                    "immediateTaxReturnType": "",
                                    "itemCodeAbb": "日用杂品",
                                    "itemIndex": 1,
                                    "itemSelfCode": "1815288373638361088",
                                    "itemName": "测试合同审批功能测试后请删除2024-07租金",
                                    "itemUnit": "个",
                                    "itemPrice": "0.010000000000000000",
                                    "itemTaxRate": "0.09",
                                    "itemNum": "1.000000000000000000",
                                    "itemAmount": "0.01",
                                    "itemTaxAmount": "0.00",
                                    "itemSpec": "",
                                    "itemCode": "1060512990000000000",
                                    "isIncludeTax": "true",
                                    "invoiceLineProperty": "0",
                                    "zeroRateFlag": "",
                                    "favouredPolicyName": ""
                                }
                            ]
                        }
                    ]
                }
                """;
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONArray result = jsonObject.getJSONArray("result");
        for (int i = 0; i < result.size(); i++) {
            JSONObject jsonResult = result.getJSONObject(i);
            JSONArray additionalElementList = jsonResult.getJSONArray("additionalElementList");
            System.out.println(JSON.toJSON(additionalElementList));
            JSONArray invoiceItems = jsonResult.getJSONArray("invoiceItems");
            System.out.println(JSON.toJSON(invoiceItems));
        }
    }
}
