package cc.ivera.mapper;

import cc.ivera.common.mybatis.GeoBaseInsertMapper;
import cc.ivera.common.mybatis.GeoBaseUpdateMapper;
import cc.ivera.pojo.Location;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.base.BaseSelectMapper;
import cc.ivera.dto.QueryDTO;
import cc.ivera.vo.LocationVo;

import java.util.List;

@Repository
public interface LocationMapper extends GeoBaseInsertMapper<Location>, GeoBaseUpdateMapper<Location>, BaseSelectMapper<Location> {
    List<LocationVo> selectByRange(QueryDTO dto);
}
