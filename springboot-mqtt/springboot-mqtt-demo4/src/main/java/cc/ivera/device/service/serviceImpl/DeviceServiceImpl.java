package cc.ivera.device.service.serviceImpl;

import cc.ivera.device.entity.Device;
import cc.ivera.device.mapper.DeviceMapper;
import cc.ivera.device.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class DeviceServiceImpl implements DeviceService {

  @Autowired
  private DeviceMapper deviceMapper;

  @Override
  public boolean updateDeviceStatus(Device param) {
    return deviceMapper.updateDeviceStatus(param);
  }
}
