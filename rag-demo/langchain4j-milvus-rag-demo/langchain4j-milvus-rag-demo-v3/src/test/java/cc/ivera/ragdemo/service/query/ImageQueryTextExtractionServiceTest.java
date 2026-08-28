package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.mapper.RagImageAssetMapper;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.service.ingest.VisionStructuredAnalysisClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageQueryTextExtractionServiceTest {

    @Test
    void keepsOriginalQueryWhenImageAnalysisProducesNoText() {
        VisionStructuredAnalysisClient visionClient = mock(VisionStructuredAnalysisClient.class);
        when(visionClient.analyze(any())).thenReturn("{}");
        ImageQueryTextExtractionService service = new ImageQueryTextExtractionService(
                visionClient,
                mock(RagImageAssetMapper.class),
                new ObjectMapper()
        );
        RagRetrievalCriteria criteria = new RagRetrievalCriteria(
                "login service architecture",
                null,
                null,
                "data:image/png;base64,AA==",
                List.of("image"),
                0L,
                List.of(1L),
                "vector",
                5,
                0.0D,
                0.0D,
                1.0D,
                0.0D,
                true,
                List.of("image"),
                List.of()
        );

        assertThat(service.extractQueryText(criteria)).contains("login service architecture");
    }
}
