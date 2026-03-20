package cc.ivera.device.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Device implements Serializable {

  private String username;

  private long ts;

}
