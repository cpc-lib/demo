package cc.ivera.test.base;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

public class Demo83 {

    @Test
    public void test1() {
        String json = """
                {"code":"E0000","describe":"调用成功","result":{"total":1,"list":[{"id":28945046,"billId":"15717285446605061","billNo":"35080224091000701901","billUuid":"322d5d68518944e3a566d1743f7f612f","billStatus":"01","requestStatus":null,"billMessage":"","openStatus":1,"applySource":0,"billTime":"2024-09-04 16:02:00","taxExcludedAmount":"-0.94","taxAmount":"-0.06","taxIncludeAmount":"-1.00","blueElecInvoiceNumber":"24352000000096322399","blueInvoiceNumber":"24352000000096322399","blueInvoiceCode":"","blueInvoiceTime":"2024-09-04 15:50:51","blueInvoiceLine":"bs","sellerTaxNo":"91350800705357886D","sellerName":"龙岩交发运营管理有限公司","buyerTaxNo":"1101324RDX8RQU1","buyerName":"个人","createTime":"2024-09-04 16:02:00","updateTime":"2024-09-04 16:55:34","sellerAccount":"13459785499","buyerAccount":null,"vatUsage":"03","saleTaxUsage":"00","accountStatus":"00","departmentId":"","clerkId":null,"extensionNumber":null,"taxNum":null,"account":null,"redReason":"2","confirmAgreement":null,"confirmReason":null,"confirmTime":null,"requestSrc":0,"detail":[{"id":146760639,"billId":"15717285446605061","detailIndex":1,"blueDetailIndex":1,"goodsName":"测试商品","unit":"1","specType":"1","taxExcludedPrice":"0.9433962264151","taxExcludedAmount":"-0.94","num":"-1","taxRate":"0.06","taxAmount":"-0.06","goodsCode":"3049900000000000000","favouredPolicyFlag":null,"favouredPolicyName":"","zeroRateFlag":"","goodsCodeAbb":"现代服务","price":"1","withTaxFlag":1,"deduction":"0.00","taxIncludedAmount":null}],"orderNo":"1571728544660506","invoiceSerialNum":null,"confirmInvoicedFlag":"Y","pdfUrl":"https://inv.jss.com.cn/fp2/NtNa2rEZlUWqhfH_MlG9Nuz12fjVbGYo_vL_7Q5Hvq6jc_bwf_mocZfL50aA8Bccu9zwqd-93yPljQhFqA_TGw.pdf"}]}}
                """;
        JSONObject jsonObject = JSON.parseObject(json);
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray list = result.getJSONArray("list");
        JSONObject data = list.getJSONObject(0);
        String billId = data.getString("billId");
        String billNo = data.getString("billNo");
        String billUuid = data.getString("billUuid");
        String billStatus = data.getString("billStatus");
        System.out.println(billId);
        System.out.println(billNo);
        System.out.println(billUuid);
        System.out.println(billStatus);

    }
}
