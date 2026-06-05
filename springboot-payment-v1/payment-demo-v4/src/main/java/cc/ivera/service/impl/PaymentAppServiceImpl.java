package cc.ivera.service.impl;

import cc.ivera.entity.PaymentApp;
import cc.ivera.mapper.PaymentAppMapper;
import cc.ivera.service.PaymentAppService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentAppServiceImpl extends ServiceImpl<PaymentAppMapper, PaymentApp> implements PaymentAppService {

    @Override
    public PaymentApp getByAppCode(String appCode) {
        LambdaQueryWrapper<PaymentApp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentApp::getAppCode, appCode);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public List<PaymentApp> listByChannelId(Long channelId) {
        LambdaQueryWrapper<PaymentApp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentApp::getChannelId, channelId)
                .orderByAsc(PaymentApp::getSortOrder);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<PaymentApp> listEnabledApps() {
        LambdaQueryWrapper<PaymentApp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentApp::getAppStatus, "ENABLED")
                .orderByAsc(PaymentApp::getSortOrder);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<PaymentApp> listEnabledAppsByChannelId(Long channelId) {
        LambdaQueryWrapper<PaymentApp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentApp::getChannelId, channelId)
                .eq(PaymentApp::getAppStatus, "ENABLED")
                .orderByAsc(PaymentApp::getSortOrder);
        return baseMapper.selectList(queryWrapper);
    }
}