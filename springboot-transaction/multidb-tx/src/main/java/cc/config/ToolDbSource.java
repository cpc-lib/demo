package cc.ivera.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ToolDbSource {

    private String id;

    private String dbSourceName;

    private String driverClassName;

    private String jbcUrl;

    private String username;

    private String password;


}
