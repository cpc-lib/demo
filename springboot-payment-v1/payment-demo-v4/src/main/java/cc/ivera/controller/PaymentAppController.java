package cc.ivera.controller;

import cc.ivera.entity.PaymentApp;
import cc.ivera.entity.PaymentChannel;
import cc.ivera.service.PaymentAppService;
import cc.ivera.service.PaymentChannelService;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/api/payment-app")
@Api(tags = "支付应用管理")
@Slf4j
public class PaymentAppController {

    private final PaymentAppService paymentAppService;
    private final PaymentChannelService paymentChannelService;

    public PaymentAppController(PaymentAppService paymentAppService, PaymentChannelService paymentChannelService) {
        this.paymentAppService = paymentAppService;
        this.paymentChannelService = paymentChannelService;
    }

    @ApiOperation("获取所有启用的支付应用列表（带渠道信息）")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> listEnabledApps() {
        log.info("获取支付应用列表");
        
        List<PaymentChannel> channels = paymentChannelService.listEnabledChannels();
        Map<Long, String> channelCodeMap = channels.stream()
                .collect(Collectors.toMap(PaymentChannel::getId, PaymentChannel::getChannelCode));
        
        List<PaymentApp> apps = paymentAppService.listEnabledApps();
        
        List<Map<String, Object>> result = apps.stream()
                .map(app -> {
                    Map<String, Object> map = Map.of(
                            "id", app.getId(),
                            "appName", app.getAppName(),
                            "appCode", app.getAppCode(),
                            "appStatus", app.getAppStatus(),
                            "channelId", app.getChannelId(),
                            "channelCode", channelCodeMap.getOrDefault(app.getChannelId(), "UNKNOWN"),
                            "appDesc", app.getAppDesc(),
                            "sortOrder", app.getSortOrder()
                    );
                    return map;
                })
                .collect(Collectors.toList());
        
        return R.ok().setData(result);
    }

    @ApiOperation("根据渠道ID获取启用的支付应用列表")
    @GetMapping("/list-by-channel/{channelId}")
    public R<List<PaymentApp>> listByChannelId(@PathVariable Long channelId) {
        log.info("根据渠道ID获取支付应用列表，channelId={}", channelId);
        List<PaymentApp> apps = paymentAppService.listEnabledAppsByChannelId(channelId);
        return R.ok().setData(apps);
    }

    @ApiOperation("获取支付应用详情")
    @GetMapping("/{id}")
    public R<PaymentApp> getById(@PathVariable Long id) {
        log.info("获取支付应用详情，id={}", id);
        PaymentApp app = paymentAppService.getById(id);
        if (app == null) {
            return R.error("支付应用不存在");
        }
        return R.ok().setData(app);
    }

    @ApiOperation("获取所有支付渠道")
    @GetMapping("/channels")
    public R<List<PaymentChannel>> listChannels() {
        log.info("获取支付渠道列表");
        List<PaymentChannel> channels = paymentChannelService.listEnabledChannels();
        return R.ok().setData(channels);
    }
}