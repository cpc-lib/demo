package cc.ivera.device.mapper;

import cc.ivera.device.entity.Device;
import org.springframework.stereotype.Repository;
@Repository
public interface DeviceMapper {

  boolean updateDeviceStatus(Device param);

}
