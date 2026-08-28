package cc.ivera.ragdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationYamlSecurityDefaultsTest {

    @Test
    void localApplicationYamlDoesNotAllowDemoTenantFallbackByDefault() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));

        assertThat(factory.getObject())
                .isNotNull()
                .containsEntry("rag.tenant.allow-demo-tenant-fallback", "${RAG_TENANT_ALLOW_DEMO_FALLBACK:false}");
    }

    @Test
    void localApplicationYamlKeepsLoginEndpointPublic() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));

        assertThat(factory.getObject())
                .isNotNull()
                .containsValue("/api/auth/login");
    }
}
