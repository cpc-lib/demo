package cc.ivera.ragdemo.service.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilvusStoreConfig {
    private String alias;
    private String host;
    private int port;
    private String collection;
    private int topK;
    private double minScore;
}
