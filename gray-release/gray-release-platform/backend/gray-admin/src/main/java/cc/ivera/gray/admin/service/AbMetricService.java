package cc.ivera.gray.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.gray.admin.entity.AbMetric;
import cc.ivera.gray.admin.mapper.AbMetricMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbMetricService {
    private final AbMetricMapper abMetricMapper;

    public AbMetricService(AbMetricMapper abMetricMapper) {
        this.abMetricMapper = abMetricMapper;
    }

    public List<AbMetric> list(String serviceId, String experimentKey) {
        return abMetricMapper.selectList(new LambdaQueryWrapper<AbMetric>()
                .eq(AbMetric::getServiceId, serviceId)
                .eq(AbMetric::getExperimentKey, experimentKey)
                .orderByAsc(AbMetric::getVariant));
    }

    @Transactional
    public AbMetric record(String serviceId, String experimentKey, String variant, boolean converted) {
        AbMetric metric = abMetricMapper.selectOne(new LambdaQueryWrapper<AbMetric>()
                .eq(AbMetric::getServiceId, serviceId)
                .eq(AbMetric::getExperimentKey, experimentKey)
                .eq(AbMetric::getVariant, variant)
                .last("limit 1"));
        if (metric == null) {
            metric = new AbMetric();
            metric.setServiceId(serviceId);
            metric.setExperimentKey(experimentKey);
            metric.setVariant(variant);
            metric.setExposures(0L);
            metric.setConversions(0L);
            abMetricMapper.insert(metric);
        }
        metric.setExposures(metric.getExposures() + 1);
        if (converted) {
            metric.setConversions(metric.getConversions() + 1);
        }
        abMetricMapper.updateById(metric);
        return abMetricMapper.selectById(metric.getId());
    }
}

