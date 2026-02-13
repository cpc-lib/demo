package cc.ivera.userdemo.kafka;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CacheWarmRequest {
  private String reqId;
  private String uid;
  private String reason;
  private List<String> want;
  private long ts;
}
