package cc.ivera.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("t_user")
@CompoundIndexes({
        @CompoundIndex(name = "idx_status_updatedAt", def = "{'status': 1, 'updatedAt': -1}"),
        @CompoundIndex(name = "idx_createdAt", def = "{'createdAt': -1}")
})
public class UserDoc {

    @Id
    private String id;

    @Indexed(unique = true)
    private String uid;

    @Indexed
    private String phone;

    @Indexed
    private String email;

    @Indexed
    private String nickname;

    @Indexed
    private Integer status; // 1=normal, 0=disabled

    private Instant createdAt;
    private Instant updatedAt;
}
