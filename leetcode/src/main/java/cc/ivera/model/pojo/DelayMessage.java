package cc.ivera.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DelayMessage implements Serializable {
    //切记实例化
    private static final long serialVersionUID = -7671756385477179547L;
    /**
     * 消息id
     */
    private String id;
    /**
     * 消息内容
     */
    private String content;
    /**
     * 到期时间
     */
    private long expireTime;
}