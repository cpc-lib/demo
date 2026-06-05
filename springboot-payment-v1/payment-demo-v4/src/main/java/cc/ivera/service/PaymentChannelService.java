package cc.ivera.service;

import cc.ivera.entity.PaymentChannel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PaymentChannelService extends IService<PaymentChannel> {

    /**
     * 根据渠道编码查询渠道信息
     *
     * @param channelCode 渠道编码
     * @return 渠道信息
     */
    PaymentChannel getByChannelCode(String channelCode);

    /**
     * 获取所有启用的渠道
     *
     * @return 渠道列表
     */
    List<PaymentChannel> listEnabledChannels();
}