package cc.ivera.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class MemberVip {
    private Integer id;
    private String name;
    private String gender;
    private String birthday;
    // lombok 会生成getter/setter方法jav
}
