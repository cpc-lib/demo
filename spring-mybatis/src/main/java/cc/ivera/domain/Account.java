package cc.ivera.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Account {
    private String username;
    private String password;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("username", getPassword())
                .append("password",getPassword())
                .toString();
    }
}
