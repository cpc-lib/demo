package cc.ivera.service;

import org.springframework.beans.BeanUtils;
import cc.ivera.common.mybatis.Geometry;
import cc.ivera.dto.LocationDTO;
import cc.ivera.mapper.LocationMapper;
import cc.ivera.pojo.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cc.ivera.dto.QueryDTO;
import cc.ivera.vo.LocationVo;

import java.util.List;
import java.util.UUID;

@Service
public class LocationService {

    @Autowired
    private LocationMapper locationMapper;

    public List<Location> selectAllLocation() {
        return locationMapper.selectAll();
    }

    public int addLocation(LocationDTO locationDto) {
        String id = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
        Location location = new Location();
        BeanUtils.copyProperties(locationDto, location);
        location.setId(id);
        location.setGis(new Geometry(locationDto.getLongitude(), locationDto.getLatitude()));
        return locationMapper.insert(location);
    }

    public List<LocationVo> selectByRange(QueryDTO dto) {
        return locationMapper.selectByRange(dto);
    }
}
