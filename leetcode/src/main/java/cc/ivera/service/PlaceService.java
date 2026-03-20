package cc.ivera.service;


import cc.ivera.model.pojo.Place;

import java.util.List;

public interface PlaceService {
    List<Place> load(Integer pid);
}
