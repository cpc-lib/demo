package cc.ivera.hotel.service.impl;

import cc.ivera.hotel.mapper.HotelMapper;
import cc.ivera.hotel.pojo.Hotel;
import cc.ivera.hotel.service.IHotelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
}
