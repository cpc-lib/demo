package cc.ivera.hotel.service;

import cc.ivera.hotel.pojo.Hotel;
import cc.ivera.hotel.pojo.PageResult;
import cc.ivera.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {
    PageResult search(RequestParams params);

    Map<String, List<String>> filters(RequestParams params);

    List<String> getSuggestions(String prefix);

    void deleteById(Long id);

    void insertById(Long id);
}
