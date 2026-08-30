package cc.ivera.service.impl.wxpay;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("微信账单多应用配置测试")
class WxPayBillServiceTest {

    @Test
    @DisplayName("指定paymentAppId时申请和下载均使用该应用凭证")
    void testDownloadBillUsesRequestedPaymentAppConfig() throws Exception {
        PaymentConfigLoader configLoader = mock(PaymentConfigLoader.class);
        WxPayHttpClient httpClient = mock(WxPayHttpClient.class);
        PaymentAppConfig requested = new PaymentAppConfig();
        requested.setAppId(77L);
        requested.setChannelCode(PaymentConfigLoader.CHANNEL_WXPAY);
        requested.setDomain("https://api.mch.weixin.qq.com");
        when(configLoader.getRequiredAppConfig(77L)).thenReturn(requested);
        when(httpClient.get(eq(requested), anyString(), anyString()))
                .thenReturn("{\"download_url\":\"https://download.example/bill\"}");
        when(httpClient.getNoSign(eq(requested), eq("https://download.example/bill"), anyString()))
                .thenReturn("bill-content");
        WxPayBillService service = new WxPayBillService(
                mock(WxPayConfig.class), configLoader, httpClient);

        String content = service.downloadBill(
                77L, LocalDate.now().minusDays(2).toString(),
                "tradebill", "ALL", null, null);

        assertThat(content).isEqualTo("bill-content");
        verify(configLoader).getRequiredAppConfig(77L);
        verify(configLoader, never()).getDefaultAppConfigByChannelCode(anyString());
        verify(httpClient).get(eq(requested), anyString(), eq("申请微信账单异常"));
        verify(httpClient).getNoSign(
                eq(requested), eq("https://download.example/bill"), eq("下载微信账单异常"));
    }
}
