package cc.ivera.controller;

import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.PaymentApp;
import cc.ivera.entity.PaymentChannel;
import cc.ivera.service.PaymentAppService;
import cc.ivera.service.PaymentChannelService;
import cc.ivera.vo.R;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentAppControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void enabledAppListRedactsSecretsButAdminListKeepsConfiguration() {
        PaymentAppService appService = mock(PaymentAppService.class);
        PaymentChannelService channelService = mock(PaymentChannelService.class);
        PaymentConfigLoader configLoader = mock(PaymentConfigLoader.class);
        PaymentApp app = new PaymentApp();
        app.setId(7L);
        app.setAppName("微信收款");
        app.setAppCode("WX_MAIN");
        app.setChannelId(9L);
        app.setAppConfig("{\"privateKey\":\"secret\"}");
        PaymentChannel channel = new PaymentChannel();
        channel.setId(9L);
        channel.setChannelCode(PaymentConfigLoader.CHANNEL_WXPAY);
        channel.setChannelName("微信支付");
        when(appService.listEnabledApps()).thenReturn(List.of(app));
        when(appService.listAllApps()).thenReturn(List.of(app));
        when(channelService.listEnabledChannels()).thenReturn(List.of(channel));
        when(channelService.listAllChannels()).thenReturn(List.of(channel));

        PaymentAppController controller = new PaymentAppController(appService, channelService, configLoader);
        Map<String, Object> enabled = ((List<Map<String, Object>>) controller.listEnabledApps().getData()).get(0);
        Map<String, Object> all = ((List<Map<String, Object>>) controller.listAllApps().getData()).get(0);

        assertEquals("WXPAY", enabled.get("channelCode"));
        assertFalse(enabled.containsKey("appConfig"));
        assertTrue(all.containsKey("appConfig"));
        assertEquals(app.getAppConfig(), all.get("appConfig"));
    }
}
