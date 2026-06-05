package cc.ivera.service.impl;

import cc.ivera.entity.PaymentChannel;
import cc.ivera.mapper.PaymentChannelMapper;
import cc.ivera.service.PaymentChannelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentChannelServiceImpl extends ServiceImpl<PaymentChannelMapper, PaymentChannel> implements PaymentChannelService {

    @Override
    public PaymentChannel getByChannelCode(String channelCode) {
        LambdaQueryWrapper<PaymentChannel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentChannel::getChannelCode, channelCode);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public List<PaymentChannel> listEnabledChannels() {
        LambdaQueryWrapper<PaymentChannel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentChannel::getChannelStatus, "ENABLED")
                .orderByAsc(PaymentChannel::getSortOrder);
        return baseMapper.selectList(queryWrapper);
    }
}