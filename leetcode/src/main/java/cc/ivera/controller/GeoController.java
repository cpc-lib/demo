package cc.ivera.controller;

import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.model.pojo.GeoQuery;

import javax.annotation.Resource;


@RestController
@RequestMapping(value = "/geo")
public class GeoController {

    private static final String GEO_KEY = "TEST:LOCATION";
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @RequestMapping("/init")
    public void init() {
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(118.79581, 32.02636), "夫子庙风光带");
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(118.79400, 32.01800), "老门东");
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(118.78787, 32.00351), "雨花台");
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(118.84109, 32.05200), "明孝陵");
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(118.85913, 32.06904), "中山陵");
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(119.01724, 32.08044), "江苏园博园");
    }

    @RequestMapping("/test0")
    public void test0(@RequestBody GeoQuery geoQuery) {
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(geoQuery.getX(), geoQuery.getY()), geoQuery.getMember());
        System.out.println("成功添加：" + geoQuery.getMember() + "，坐标：" + geoQuery.getX() + "，" + geoQuery.getY());
    }

    @RequestMapping("/test1")
    public void test1(@RequestBody GeoQuery geoQuery) {
        Circle circle = new Circle(new Point(geoQuery.getX(), geoQuery.getY()), new Distance(geoQuery.getValue(), RedisGeoCommands.DistanceUnit.METERS));
        // 按近距离排序，查询前10条
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates().sortAscending().limit(10);
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo().radius(GEO_KEY, circle, args);
        System.out.println("相距坐标：" + geoQuery.getX() + "，" + geoQuery.getY() + "，" + geoQuery.getValue() + "米范围内景点如下：");
        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult : results) {
            System.out.println("景点名称：" + geoResult.getContent().getName() + "，相距：" + geoResult.getDistance().getValue() + "米");
        }
    }

    @RequestMapping("/test2")
    public void test2(@RequestBody GeoQuery geoQuery) {
        Distance distance = redisTemplate.opsForGeo().distance(GEO_KEY, geoQuery.getMember1(), geoQuery.getMember2(), RedisGeoCommands.DistanceUnit.METERS);
        System.out.println(geoQuery.getMember1() + "，" + geoQuery.getMember2() + "，相距：" + distance.getValue() + "米");
    }

    @RequestMapping("/test3")
    public void test3(@RequestBody GeoQuery geoQuery) {
        // 按近距离排序，查询前10条
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates().sortAscending().limit(10);
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisTemplate.opsForGeo().radius(GEO_KEY, geoQuery.getMember(), new Distance(geoQuery.getValue(), RedisGeoCommands.DistanceUnit.METERS), args);

        System.out.println("相距景点：" + geoQuery.getMember() + "，" + geoQuery.getValue() + "米范围内景点如下：");

        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult : results) {
            System.out.println("景点名称：" + geoResult.getContent().getName() + "，相距：" + geoResult.getDistance().getValue() + "米");
        }
    }

}
