package cc.ivera.domain;

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

    private String id;       //消息id
    private String content;  //消息内容
    private long expireTime; //到期时间
}