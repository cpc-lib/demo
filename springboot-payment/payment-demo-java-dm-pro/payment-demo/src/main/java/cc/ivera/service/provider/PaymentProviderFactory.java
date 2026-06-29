package cc.ivera.service.provider;

import cc.ivera.enums.PayType;
import cc.ivera.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付提供商工厂 — 根据支付类型返回对应的 PaymentProvider。
 *
 * 所有 PaymentProvider 实现类在 Spring 容器初始化时自动注册。
 */
@Component
public class PaymentProviderFactory {

    private final Map<String, PaymentProvider> providerMap = new ConcurrentHashMap<>();

    public PaymentProviderFactory(List<PaymentProvider> providers) {
        for (PaymentProvider provider : providers) {
            String type = provider.getPaymentType();
            if (type != null) {
                providerMap.put(type, provider);
            }
        }
    }

    /**
     * 根据支付类型获取对应的 Provider。
     *
     * @param paymentType 支付类型（WXPAY / ALIPAY）
     * @return 对应的 PaymentProvider
     * @throws BizException 如果找不到对应的 Provider
     */
    public PaymentProvider getProvider(String paymentType) {
        PaymentProvider provider = providerMap.get(paymentType);
        if (provider == null) {
            throw new BizException("不支持的支付类型: " + paymentType);
        }
        return provider;
    }

    /**
     * 判断是否支持指定的支付类型。
     */
    public boolean supports(String paymentType) {
        return providerMap.containsKey(paymentType);
    }
}
