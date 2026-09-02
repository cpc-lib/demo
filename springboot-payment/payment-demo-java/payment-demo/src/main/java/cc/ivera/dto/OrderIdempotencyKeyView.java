package cc.ivera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderIdempotencyKeyView {

    private String idempotencyKey;

    private Date expiresAt;
}
