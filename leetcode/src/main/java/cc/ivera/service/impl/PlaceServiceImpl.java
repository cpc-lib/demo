package cc.ivera.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cc.ivera.mapper.PlaceMapper;
import cc.ivera.model.pojo.Place;
import cc.ivera.service.PlaceService;
import cc.ivera.util.TreeUtil;

import java.util.List;


@Service
@Slf4j
public class PlaceServiceImpl implements PlaceService {

    @Autowired
    private PlaceMapper placeMapper;

    public List<Place> load(Integer pid) {
        return TreeUtil.toTree(placeMapper.load(), pid, 1, 1);
    }


}
