package cc.ivera.service.impl;

import cc.ivera.config.DataSource;
import cn.hutool.core.util.ObjectUtil;
import cc.ivera.domain.Good;
import cc.ivera.mapper.GoodMapper;
import cc.ivera.service.GoodService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GoodServiceImpl extends BaseServiceImpl implements GoodService {

    @Autowired
    private GoodMapper goodMapper;


    @Override
    @DataSource("default")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int insert(Good good) {
        return goodMapper.insert(good);
    }

    /***
     * 使用synchronized可以不使用乐观锁
     * @param goodId
     * @return
     */
    @Override
    @DataSource("default")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public synchronized Map<String, Object> sellGood1(String goodId) {
        //首先查询库存，判断库存量是否大于>0,
        //是库存量-1,设置回去，版本号+1(SQL语句)
        Good good = goodMapper.selectById(goodId);
        Long total = good.getTotal();
        Long version = 0L;
        Long afterBuying;
        Boolean flag = false;
        Map<String, Object> map = new HashMap<>();
        if (ObjectUtil.isNotNull(total)) {
            if (total > 0L) {
                afterBuying = total - 1L;
                //乐观锁，确保不超卖，但是不能实现我想要的结果输出，因此添加了锁
                version = good.getVersion();
                Integer count = goodMapper.updateGoodTotal(afterBuying, version, goodId);
                if (ObjectUtil.isNull(count) || 0 == count) {
                    map.put("flag", flag);
                }else {
                    map.put("flag", !flag);
                }
            } else {
                map.put("flag", flag);
            }
            map.put("beforeBuying",total);
            //!!!再次查询数据库，获取当前最新的库存量
            map.put("afterBuying", goodMapper.selectById(goodId).getTotal());
            return map;
        }
        return map;
    }


    @Override
    @DataSource("default")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public  Map<String, Object> sellGood2(String goodId) {
        Good good = goodMapper.selectById(goodId);
        Long total = good.getTotal();
        Long afterBuying;
        Boolean flag = false;
        Map<String, Object> map = new HashMap<>();
        if (ObjectUtil.isNotNull(total)) {
            if (total > 0L) {
                afterBuying = total - 1L;
                good.setTotal(afterBuying);
                good.setVersion(good.getVersion());
                Integer count = goodMapper.updateById(good);
                if (ObjectUtil.isNull(count) || 0 == count) {
                    map.put("flag", flag);
                }else {
                    map.put("flag", !flag);
                }
            } else {
                map.put("flag", flag);
            }
            map.put("beforeBuying",total);
            //!!!再次查询数据库，获取当前最新的库存量
            map.put("afterBuying", goodMapper.selectById(goodId).getTotal());
            return map;
        }
        return map;
    }



    @Override
    @DataSource("default")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public  Map<String, Object> sellGood3(String goodId) throws InterruptedException {
        Good good = goodMapper.selectByGoodId(goodId);
        Long total = good.getTotal();
        Long afterBuying;
        Boolean flag = false;
        Map<String, Object> map = new HashMap<>();
        if (ObjectUtil.isNotNull(total)) {
            if (total > 0L) {
                afterBuying = total - 1L;
                good.setTotal(afterBuying);
                //版本控制
                //good.setVersion(good.getVersion());
                //Thread.sleep(500);
                Integer count = goodMapper.updateByGood(good);
                if (ObjectUtil.isNull(count) || 0 == count) {
                    map.put("flag", flag);
                }else {
                    map.put("flag", !flag);
                }
            } else {
                map.put("flag", flag);
            }
            map.put("beforeBuying",total);
            //!!!再次查询数据库，获取当前最新的库存量
            map.put("afterBuying", goodMapper.selectById(goodId).getTotal());
            return map;
        }
        return map;
    }
}
