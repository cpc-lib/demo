package cc.ivera.service.impl;

import cc.ivera.service.PaymentInfoService;
import cc.ivera.entity.PaymentInfo;
import cc.ivera.enums.PayType;
import cc.ivera.mapper.PaymentInfoMapper;
import cc.ivera.util.JsonUtils;
import cc.ivera.util.MoneyUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    /**
     * 记录支付日志：微信支付
     *
     * @param plainText
     */
    @Override
    public void createPaymentInfo(String plainText) {
        log.info("记录支付日志");

        Map<String, Object> plainTextMap = JsonUtils.toObjectMap(plainText);

        //订单号
        String orderNo = (String) plainTextMap.get("out_trade_no");
        //业务编号
        String transactionId = (String) plainTextMap.get("transaction_id");
        //支付类型
        String tradeType = (String) plainTextMap.get("trade_type");
        //交易状态
        String tradeState = (String) plainTextMap.get("trade_state");
        //用户实际支付金额
        Map<String, Object> amount = JsonUtils.toObjectMap(plainTextMap.get("amount"));
        Integer payerTotal = amount == null ? null : toInteger(amount.get("payer_total"));

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(payerTotal);
        paymentInfo.setContent(plainText);

        baseMapper.insert(paymentInfo);
    }

    /**
     * 记录支付日志：微信支付 APIv2
     *
     * @param params 通知参数
     * @param content 原始通知内容
     */
    @Override
    public void createPaymentInfoForWxPayV2(Map<String, String> params, String content) {
        log.info("记录支付日志");

        String totalFee = params.get("total_fee");

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(params.get("out_trade_no"));
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(params.get("transaction_id"));
        paymentInfo.setTradeType(params.get("trade_type"));
        paymentInfo.setTradeState(params.get("result_code"));
        paymentInfo.setPayerTotal(totalFee == null ? null : Integer.valueOf(totalFee));
        paymentInfo.setContent(content);

        baseMapper.insert(paymentInfo);
    }

    /**
     * 记录支付日志：支付宝
     *
     * @param params
     */
    @Override
    public void createPaymentInfoForAliPay(Map<String, ?> params) {
        log.info("记录支付日志");

        //获取订单号
        String orderNo = getString(params, "out_trade_no");
        //业务编号
        String transactionId = getString(params, "trade_no");
        //交易状态
        String tradeStatus = getString(params, "trade_status");
        //交易金额
        String totalAmount = getString(params, "total_amount");
        int totalAmountInt = MoneyUtils.yuanToCents(totalAmount);

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.ALIPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType("电脑网站支付");
        paymentInfo.setTradeState(tradeStatus);
        paymentInfo.setPayerTotal(totalAmountInt);

        paymentInfo.setContent(JsonUtils.toJson(params));

        baseMapper.insert(paymentInfo);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private String getString(Map<String, ?> params, String key) {
        Object value = params.get(key);
        return value == null ? null : value.toString();
    }
}
