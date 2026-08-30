package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_refresh_token")
public class RefreshToken extends BaseEntity {

    private Long userId;

    private String tokenHash;

    private String tokenFamily;

    private Date expiresAt;

    private Date revokedAt;

    private String replacedByHash;
}
