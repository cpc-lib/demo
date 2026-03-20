package cc.ivera.provider.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.provider.domain.Goods;
import cc.ivera.provider.service.GoodsService;

/**
 * Goods Controller 服务提供方
 */

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Value("${server.port}")
    private int port;


    /**
     * @param id
     * @return
     * findOne_fallback 降级方法,如果执行失败，则调用此方法 如果没有定义此方法,消费方调用FeignClientFallback中复写的方法
     */
    @GetMapping("/findOne/{id}")
    @HystrixCommand(fallbackMethod = "findOne_fallback", commandProperties = {
            //设置Hystrix的超时时间，默认1s
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000"),
            //监控时间 默认5000 毫秒
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000"),
            //失败次数。默认20次
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "20"),
            //失败率 默认50%
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50")

    })
    public Goods findOne(@PathVariable("id") int id) {


        //如果id == 1 ，则出现异常，id != 1 则正常访问
        if (id == 1) {
            //1.造个异常
            int i = 3 / 0;
        }

        /*try {
            //2. 休眠2秒
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        Goods goods = goodsService.findOne(id);

        goods.setTitle(goods.getTitle() + ":" + port);//将端口号，设置到了 商品标题上
        return goods;

    }

    public Goods findOne_fallback(int id) {
        Goods goods = new Goods();
        goods.setTitle("系统错误或服务忙进行服务降级处理2");
        return goods;
    }
}
