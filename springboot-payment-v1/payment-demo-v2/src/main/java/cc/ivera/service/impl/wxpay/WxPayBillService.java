package cc.ivera.service.impl.wxpay;

import cc.ivera.config.WxPayConfig;
import cc.ivera.enums.wxpay.WxApiType;
import cc.ivera.exception.BizException;
import cc.ivera.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class WxPayBillService {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private WxPayHttpClient wxPayHttpClient;

    public String queryBill(String billDate, String type) {
        log.warn("申请账单接口调用 {}", billDate);

        String apiPath;
        if ("tradebill".equals(type)) {
            apiPath = WxApiType.TRADE_BILLS.getType();
        } else if ("fundflowbill".equals(type)) {
            apiPath = WxApiType.FUND_FLOW_BILLS.getType();
        } else {
            throw new BizException("不支持的账单类型");
        }

        String url = wxPayConfig.getDomain().concat(apiPath).concat("?bill_date=").concat(billDate);
        String bodyAsString;
        try {
            bodyAsString = wxPayHttpClient.get(url, "申请账单异常");
        } catch (IOException e) {
            throw new BizException("申请账单异常", e);
        }

        Map<String, Object> resultMap = JsonUtils.toObjectMap(bodyAsString);
        return getString(resultMap, "download_url");
    }

    public String downloadBill(String billDate, String type) {
        log.warn("下载账单接口调用 {}, {}", billDate, type);

        String downloadUrl = queryBill(billDate, type);
        try {
            return wxPayHttpClient.getNoSign(downloadUrl, "下载账单异常");
        } catch (IOException e) {
            throw new BizException("下载账单异常", e);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
