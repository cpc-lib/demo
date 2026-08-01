package cc.ivera.ragdemo.service.ingest;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.ContentType;
import cc.ivera.ragdemo.model.knowledge.ExtractedImageKnowledge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisionStructuredAnalysisClientTest {

    @Test
    void acceptsDataUrlImageInputForAnalysis() {
        RagProperties properties = new RagProperties();
        properties.getMultimodalIngest().setEnabled(true);
        properties.getMultimodalIngest().setVisionAnalysisEnabled(true);
        properties.getMultimodalIngest().setVisionBaseUrl("http://127.0.0.1:1");
        properties.getMultimodalIngest().setVisionApiKey("test-key");
        properties.getMultimodalIngest().setVisionModel("vision-test");
        properties.getMultimodalIngest().setTimeoutSeconds(1);

        VisionStructuredAnalysisClient client = new VisionStructuredAnalysisClient(properties, new ObjectMapper());

        String result = client.analyze(new ExtractedImageKnowledge(
                "query-image",
                ContentType.IMAGE,
                null,
                "data:image/png;base64,AA==",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(result).contains("\"analysisStatus\":\"failed\"");
    }
}
