package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.service.ragops.ProductionConfigurationPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultProfileConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(RagPropertiesConfiguration.class);

    @Test
    void defaultConfigurationDoesNotActivateProfileSpecificFiles() {
        Properties application = yamlProperties("application.yml");

        assertThat(application.getProperty("spring.config.import")).isNull();
        assertThat(application.getProperty("spring.profiles.active")).isNull();
        assertThat(Files.exists(Path.of("src/main/resources/application-dev.yml"))).isFalse();
    }

    @Test
    void baseConfigurationUsesDemoSecurityDefaults() {
        Properties application = yamlProperties("application.yml");

        assertThat(application.getProperty("rag.security.mode")).isEqualTo("dev");
        assertThat(application.getProperty("rag.tenant.dev-header-enabled")).isEqualTo("true");
        assertThat(application.getProperty("rag.security.api-key-encryption-key"))
                .isEqualTo("${RAG_SECURITY_API_KEY_ENCRYPTION_KEY:rag-demo-default-encryption-key}");
        assertThat(application.getProperty("rag.chunk-registry.rebuild-on-startup"))
                .isEqualTo("${RAG_CHUNK_REGISTRY_REBUILD_ON_STARTUP:false}");
        assertThat(application.getProperty("rag.schema.auto-initialize"))
                .isEqualTo("${RAG_SCHEMA_AUTO_INITIALIZE:true}");
        assertThat(application.getProperty("rag.schema.bootstrap-location"))
                .isEqualTo("${RAG_SCHEMA_BOOTSTRAP_LOCATION:classpath:sql/all-in-one.sql}");
    }

    @Test
    void defaultConfigurationBindsBaseDemoSettings() {
        contextRunner.run(context -> {
            RagProperties properties = context.getBean(RagProperties.class);

            assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
            assertThat(properties.getSecurity().getMode()).isEqualTo("dev");
            assertThat(properties.getTenant().isDevHeaderEnabled()).isTrue();
            assertThat(properties.getTenant().getDemoRoles()).contains("TENANT_ADMIN", "KB_OWNER");
            assertThat(properties.getTenant().getDemoRoles()).doesNotContain("PLATFORM_ADMIN", "SUPER_ADMIN");
            assertThat(properties.getSecurity().getApiKeyEncryptionKey())
                    .isEqualTo("rag-demo-default-encryption-key");
            assertThat(properties.getChunkRegistry().isRebuildOnStartup()).isFalse();
            assertThat(properties.getSchema().isAutoInitialize()).isTrue();
            assertThat(properties.getSchema().getBootstrapLocation()).isEqualTo("classpath:sql/all-in-one.sql");
            assertThat(properties.getMilvus().getHost()).isNotBlank();
            assertThat(properties.getMilvus().getRpcDeadlineMs()).isZero();
            assertThat(new ProductionConfigurationPolicy().violations(properties, false)).isEmpty();
        });
    }

    @Test
    void productionConfigurationDoesNotAutoInitializeSchema() {
        Properties production = yamlProperties("application-prod.yml");

        assertThat(production.getProperty("rag.schema.auto-initialize"))
                .isEqualTo("${RAG_SCHEMA_AUTO_INITIALIZE:false}");
    }

    private Properties yamlProperties(String resourceName) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));
        Properties properties = factory.getObject();
        assertThat(properties).as(resourceName + " can be loaded").isNotNull();
        return properties;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RagProperties.class)
    static class RagPropertiesConfiguration {
    }
}
