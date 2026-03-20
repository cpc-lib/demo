package cc.ivera.service;

import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import cc.ivera.model.pojo.GeoRadiusInfo;
import cc.ivera.model.pojo.GeoRadiusQuery;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class RedisService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public long geoAdd(String key, double longitude, double latitude, String member) {
        Long num = redisTemplate.opsForGeo().add(key, new Point(longitude, latitude), member);
        if (num == null) {
            return -1;
        }
        return num;
    }

    public List<GeoRadiusInfo> radius(GeoRadiusQuery geoRadiusQuery) {
        Circle circle = new Circle(
                new Point(geoRadiusQuery.getLongitude(), geoRadiusQuery.getLatitude()),
                new Distance(geoRadiusQuery.getValue(), geoRadiusQuery.getMetric()));

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates();

        if (geoRadiusQuery.getSort() == 1) {
            args.sortDescending();
        } else {
            args.sortAscending();
        }

        args.limit(geoRadiusQuery.getLimit() > 0 ? geoRadiusQuery.getLimit() : 10);

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo().radius(geoRadiusQuery.getKey(), circle, args);

        List<GeoRadiusInfo> result = new ArrayList<>();

        if (results == null) {
            return result;
        }

        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult : results) {
            GeoRadiusInfo geoRadiusInfo = new GeoRadiusInfo();
            geoRadiusInfo.setName(String.valueOf(geoResult.getContent().getName()));
            geoRadiusInfo.setLongitude(geoResult.getContent().getPoint().getX());
            geoRadiusInfo.setLatitude(geoResult.getContent().getPoint().getY());
            geoRadiusInfo.setValue(geoResult.getDistance().getValue());
            result.add(geoRadiusInfo);
        }

        return result;
    }

    public double distance(String key, String member1, String member2, Metric metric) {
        Distance distance = redisTemplate.opsForGeo().distance(key, member1, member2, metric);
        return distance == null ? -1 : distance.getValue();
    }


    public List<GeoRadiusInfo> radiusByMember(GeoRadiusQuery geoRadiusQuery) {

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates();

        if (geoRadiusQuery.getSort() == 1) {
            args.sortDescending();
        } else {
            args.sortAscending();
        }

        args.limit(geoRadiusQuery.getLimit() > 0 ? geoRadiusQuery.getLimit() : 10);


        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisTemplate.opsForGeo().radius(geoRadiusQuery.getKey(), geoRadiusQuery.getMember(),
                        new Distance(geoRadiusQuery.getValue(), geoRadiusQuery.getMetric()), args);

        List<GeoRadiusInfo> result = new ArrayList<>();

        if (results == null) {
            return result;
        }

        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult : results) {
            GeoRadiusInfo geoRadiusInfo = new GeoRadiusInfo();
            geoRadiusInfo.setName(String.valueOf(geoResult.getContent().getName()));
            geoRadiusInfo.setLongitude(geoResult.getContent().getPoint().getX());
            geoRadiusInfo.setLatitude(geoResult.getContent().getPoint().getY());
            geoRadiusInfo.setValue(geoResult.getDistance().getValue());
            result.add(geoRadiusInfo);
        }

        return result;
    }

}
